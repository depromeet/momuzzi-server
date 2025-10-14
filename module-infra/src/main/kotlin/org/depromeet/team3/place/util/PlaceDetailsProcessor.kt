package org.depromeet.team3.place.util

import kotlinx.coroutines.coroutineScope
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 장소 상세 정보 병렬 조회 및 DTO 변환 담당
 */
@Component
class PlaceDetailsProcessor(
    private val placeQuery: PlaceQuery,
    private val placeAddressResolver: PlaceAddressResolver,
    private val googlePlacesApiProperties: GooglePlacesApiProperties
) {
    private val logger = LoggerFactory.getLogger(PlaceDetailsProcessor::class.java)

    /**
     * 여러 장소의 상세 정보를 배치로 가져와서 PlaceDetailResult로 변환
     * (DB 캐싱 + 병렬 API 호출 + 배치 INSERT)
     */
    suspend fun fetchPlaceDetailsInParallel(
        places: List<PlacesTextSearchResponse.Place>
    ): List<PlaceDetailResult> = coroutineScope {
        try {
            // 1. openNow와 link 정보를 미리 추출 (Text Search 응답에서)
            val openNowMap = places.associate { it.id to it.currentOpeningHours?.openNow }
            val koreanNames = places.associate { 
                it.id to PlaceFormatter.extractKoreanName(it.displayName.text) 
            }
            val linkMap = koreanNames.mapValues { (_, name) -> 
                PlaceFormatter.generateNaverPlaceLink(name) 
            }
            
            // 2. 배치로 Details 조회 (DB 캐싱 + 병렬 API 호출 + 배치 저장)
            val placeIds = places.map { it.id }
            logger.debug("Place Details 배치 조회 시작: placeIds=$placeIds")
            val detailsMap = placeQuery.getPlaceDetailsBatch(placeIds, openNowMap, linkMap)
            logger.debug("Place Details 배치 조회 완료: detailsMap 크기=${detailsMap.size}")
            
            // 3. 각 place를 PlaceDetailResult로 변환
            places.mapNotNull { place ->
                val placeDetails = detailsMap[place.id]
                if (placeDetails == null) {
                    logger.warn("PlaceDetails 없음: placeId=${place.id}")
                    return@mapNotNull null
                }
                
                try {
                    val topReview = extractTopReview(placeDetails)
                    val photos = extractPhotos(placeDetails)
                    val priceRange = extractPriceRange(placeDetails)
                    val addressDescriptor = placeAddressResolver.resolveAddressDescriptor(placeDetails)
                    val koreanName = koreanNames[place.id] ?: place.displayName.text
                    val openNowValue = placeDetails.currentOpeningHours?.openNow ?: place.currentOpeningHours?.openNow
                    
                    PlaceDetailResult(
                        placeId = place.id,
                        name = koreanName,
                        address = place.formattedAddress.replace("대한민국 ", ""),
                        rating = place.rating ?: 0.0,
                        userRatingsTotal = place.userRatingCount ?: 0,
                        openNow = openNowValue,
                        photos = photos,
                        link = linkMap[place.id] ?: "",
                        weekdayText = placeDetails.regularOpeningHours?.weekdayDescriptions,
                        topReview = topReview,
                        priceRange = priceRange,
                        addressDescriptor = addressDescriptor?.description
                    )
                } catch (e: Exception) {
                    logger.warn("장소 변환 실패: placeId=${place.id}, error=${e.message}")
                    // 변환 실패 시에도 기본 정보는 반환
                    PlaceDetailResult(
                        placeId = place.id,
                        name = place.displayName.text,
                        address = place.formattedAddress.replace("대한민국 ", ""),
                        rating = place.rating ?: 0.0,
                        userRatingsTotal = place.userRatingCount ?: 0,
                        openNow = place.currentOpeningHours?.openNow,
                        photos = null,
                        link = linkMap[place.id] ?: "",
                        weekdayText = null,
                        topReview = null,
                        priceRange = null,
                        addressDescriptor = null
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("장소 상세 정보 배치 조회 실패", e)
            throw PlaceSearchException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "장소 상세 정보 조회 중 오류가 발생했습니다"
            )
        }
    }
    
    /**
     * 최고 평점 리뷰 추출
     */
    private fun extractTopReview(
        placeDetails: PlaceDetailsResponse?
    ): ReviewResult? {
        return try {
            placeDetails?.reviews
                ?.filter { it.text != null && it.text.text.isNotBlank() }
                ?.maxByOrNull { it.rating }
                ?.let { review ->
                    ReviewResult(
                        rating = review.rating.toInt(),
                        text = review.text?.text ?: ""
                    )
                }
        } catch (e: Exception) {
            logger.warn("리뷰 추출 실패: ${e.message}")
            null
        }
    }
    
    /**
     * 사진 URL 추출 (최대 5개)
     */
    private fun extractPhotos(
        placeDetails: PlaceDetailsResponse?
    ): List<String>? {
        return try {
            if (placeDetails?.photos == null) {
                logger.debug("placeDetails.photos가 null입니다. placeId: ${placeDetails?.id}")
                return null
            }
            
            if (placeDetails.photos.isEmpty()) {
                logger.debug("placeDetails.photos가 비어있습니다. placeId: ${placeDetails.id}")
                return null
            }
            
            logger.debug("사진 추출 시작. placeId: ${placeDetails.id}, photos 개수: ${placeDetails.photos.size}")
            
            val photoUrls = placeDetails.photos.take(5).mapNotNull { photo ->
                try {
                    val photoUrl = PlaceFormatter.generatePhotoUrl(photo.name, googlePlacesApiProperties.apiKey)
                    logger.debug("사진 URL 생성 성공: ${photo.name} -> $photoUrl")
                    photoUrl
                } catch (e: Exception) {
                    logger.warn("사진 URL 생성 실패: photoName=${photo.name}, error=${e.message}")
                    null
                }
            }
            
            logger.debug("사진 URL 추출 완료. placeId: ${placeDetails.id}, 생성된 URL 개수: ${photoUrls.size}")
            photoUrls
        } catch (e: Exception) {
            logger.warn("사진 추출 실패: placeId=${placeDetails?.id}, error=${e.message}")
            null
        }
    }
    
    /**
     * 가격대 추출
     */
    private fun extractPriceRange(
        placeDetails: PlaceDetailsResponse?
    ): PriceRangeResult? {
        return try {
            placeDetails?.priceRange?.let { range ->
                PriceRangeResult(
                    startPrice = formatMoney(range.startPrice),
                    endPrice = formatMoney(range.endPrice)
                )
            }
        } catch (e: Exception) {
            logger.warn("가격대 추출 실패: ${e.message}")
            null
        }
    }
    
    /**
     * Money 객체를 문자열로 포맷
     */
    private fun formatMoney(money: PlaceDetailsResponse.PriceRange.Money?): String? {
        return try {
            if (money == null) return null
            val amount = money.units ?: "0"
            "${money.currencyCode} $amount"
        } catch (e: Exception) {
            logger.warn("가격 포맷 실패: ${e.message}")
            null
        }
    }
    
    /**
     * 장소 상세 정보 결과 DTO
     */
    data class PlaceDetailResult(
        val placeId: String,
        val name: String,
        val address: String,
        val rating: Double,
        val userRatingsTotal: Int,
        val openNow: Boolean?,
        val photos: List<String>?,
        val link: String,
        val weekdayText: List<String>?,
        val topReview: ReviewResult?,
        val priceRange: PriceRangeResult?,
        val addressDescriptor: String?
    )
    
    data class ReviewResult(
        val rating: Int,
        val text: String
    )
    
    data class PriceRangeResult(
        val startPrice: String?,
        val endPrice: String?
    )
}
