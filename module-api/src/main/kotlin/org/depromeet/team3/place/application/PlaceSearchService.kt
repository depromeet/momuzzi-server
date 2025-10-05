package org.depromeet.team3.place.application

import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.springframework.stereotype.Service
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Service
class PlaceSearchService(
    private val googlePlacesClient: GooglePlacesClient,
    private val googlePlacesApiProperties: GooglePlacesApiProperties,
) {
    private val logger = LoggerFactory.getLogger(PlaceSearchService::class.java)
    
    /**
     * 검색어별 현재 offset을 관리하는 메모리 캐시
     */
    private val queryOffsetCache = Caffeine.newBuilder()
        .expireAfterAccess(60, TimeUnit.MINUTES) // 60분간 접근 없으면 제거
        .maximumSize(500)                        // 최대 500개 검색어 캐싱
        .build<String, Int>()
    
    /**
     * Google API에서 한 번에 가져올 전체 결과 개수 => 5개, 5개씩
     */
    private val totalFetchSize = 10
    private val maxCallCount = 2

    /**
     * 맛집 검색 및 순차 결과 반환
     *
     * @param request 검색 요청 (검색어, 결과 개수)
     * @return 검색 결과 목록 (최대 3번까지 다른 결과 반환)
     */
    fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse {
        val response = try {
            googlePlacesClient.textSearch(request.query, totalFetchSize)
        } catch (e: Exception) {
            logger.error("Failed to call Google Places API for query: ${request.query}", e)
            throw PlaceSearchException("맛집 검색 중 오류가 발생했습니다", e)
        }
        
        if (response == null || response.status != "OK") {
            logger.warn("Google Places API returned unsuccessful status: ${response?.status}")
            return PlacesSearchResponse(emptyList())
        }

        // 원자적 offset 조회 및 갱신
        val currentOffset = queryOffsetCache.getIfPresent(request.query) ?: 0
        val currentCallCount = currentOffset / request.maxResults

        if (currentCallCount >= maxCallCount) {
            queryOffsetCache.invalidate(request.query)
            return PlacesSearchResponse(emptyList())
        }

        val startIndex = currentOffset
        val endIndex = minOf(startIndex + request.maxResults, response.results.size)
        
        if (startIndex >= response.results.size) {
            queryOffsetCache.invalidate(request.query)
            return PlacesSearchResponse(emptyList())
        }
        
        val selectedResults = response.results.subList(startIndex, endIndex)

        val items = selectedResults.map { result ->
            val placeDetails = googlePlacesClient.getPlaceDetails(result.placeId)?.result
            
            val topReview = placeDetails?.reviews
                ?.maxByOrNull { it.rating }
                ?.let { review ->
                    PlacesSearchResponse.PlaceItem.Review(
                        rating = review.rating.toInt(),
                        text = review.text
                    )
                }
            
            PlacesSearchResponse.PlaceItem(
                name = result.name,
                address = result.formattedAddress,
                rating = result.rating,
                userRatingsTotal = result.userRatingsTotal,
                openNow = result.openingHours?.openNow,
                photos = placeDetails?.photos?.take(5)?.map { photo ->
                    generatePhotoUrl(photo.photoReference)
                },
                link = generateNaverPlaceLink(result.name),
                weekdayText = placeDetails?.openingHours?.weekdayText,
                topReview = topReview
            )
        }

        // 원자적으로 offset 증가
        queryOffsetCache.asMap().merge(request.query, request.maxResults, Int::plus)

        return PlacesSearchResponse(items)
    }

    /**
     * 네이버 플레이스 링크 생성
     */
    private fun generateNaverPlaceLink(placeName: String): String {
        return "https://m.place.naver.com/restaurant/list?query=$placeName"
    }

    /**
     * Google Places Photo URL 생성
     */
    private fun generatePhotoUrl(photoReference: String): String {
        return "${googlePlacesApiProperties.baseUrl}/photo?maxwidth=400&photo_reference=$photoReference&key=${googlePlacesApiProperties.apiKey}"
    }
}
