package org.depromeet.team3.place.util

import kotlinx.coroutines.coroutineScope
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 장소 상세 정보 병렬 조회 및 DTO 변환 담당
 */
@Component
class PlaceDetailsAssembler(
    private val placeQuery: PlaceQuery,
    private val placeAddressResolver: PlaceAddressResolver,
    private val googlePlacesApiProperties: GooglePlacesApiProperties
) {
    private val logger = LoggerFactory.getLogger(PlaceDetailsAssembler::class.java)

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
            val detailsMap = placeQuery.getPlaceDetailsBatch(placeIds, openNowMap, linkMap)
            
            // 3. 각 place를 PlaceDetailResult로 변환
            places.mapNotNull { place ->
                val placeDetails = detailsMap[place.id] ?: return@mapNotNull null
                
                val topReview = extractTopReview(placeDetails)
                val photos = extractPhotos(placeDetails)
                val priceRange = extractPriceRange(placeDetails)
                val addressDescriptor = placeAddressResolver.resolveAddressDescriptor(placeDetails)
                val koreanName = koreanNames[place.id] ?: place.displayName.text
                
                PlaceDetailResult(
                    name = koreanName,
                    address = place.formattedAddress,
                    rating = place.rating ?: 0.0,
                    userRatingsTotal = place.userRatingCount ?: 0,
                    openNow = place.currentOpeningHours?.openNow,
                    photos = photos,
                    link = linkMap[place.id] ?: "",
                    weekdayText = placeDetails.regularOpeningHours?.weekdayDescriptions,
                    topReview = topReview,
                    priceRange = priceRange,
                    addressDescriptor = addressDescriptor?.description
                )
            }
        } catch (e: Exception) {
            logger.warn("장소 상세 정보 배치 조회 실패", e)
            emptyList()
        }
    }
    
    /**
     * 최고 평점 리뷰 추출
     */
    private fun extractTopReview(
        placeDetails: PlaceDetailsResponse?
    ): ReviewResult? {
        return placeDetails?.reviews
            ?.maxByOrNull { it.rating }
            ?.let { review ->
                ReviewResult(
                    rating = review.rating.toInt(),
                    text = review.text.text
                )
            }
    }
    
    /**
     * 사진 URL 추출 (최대 5개)
     */
    private fun extractPhotos(
        placeDetails: PlaceDetailsResponse?
    ): List<String>? {
        return placeDetails?.photos?.take(5)?.map { photo ->
            PlaceFormatter.generatePhotoUrl(photo.name, googlePlacesApiProperties.apiKey)
        }
    }
    
    /**
     * 가격대 추출
     */
    private fun extractPriceRange(
        placeDetails: PlaceDetailsResponse?
    ): PriceRangeResult? {
        return placeDetails?.priceRange?.let { range ->
            PriceRangeResult(
                startPrice = formatMoney(range.startPrice),
                endPrice = formatMoney(range.endPrice)
            )
        }
    }
    
    /**
     * Money 객체를 문자열로 포맷
     */
    private fun formatMoney(money: PlaceDetailsResponse.PriceRange.Money?): String? {
        if (money == null) return null
        val amount = money.units ?: "0"
        return "${money.currencyCode} $amount"
    }
    
    /**
     * 장소 상세 정보 결과 DTO
     */
    data class PlaceDetailResult(
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
