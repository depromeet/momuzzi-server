package org.depromeet.team3.place.application

import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class PlaceSearchService(
    private val googlePlacesClient: GooglePlacesClient,
) {
    /**
     * 검색어별 현재 offset을 관리하는 메모리 캐시
     */
    private val queryOffsetMap = ConcurrentHashMap<String, Int>()
    
    /**
     * Google API에서 한 번에 가져올 전체 결과 개수
     * 3번 호출 * 5개 결과 = 15개
     */
    private val totalFetchSize = 15

    private val maxCallCount = 3

    /**
     * 맛집 검색 및 순차 결과 반환
     *
     * @param request 검색 요청 (검색어, 결과 개수)
     * @return 검색 결과 목록 (최대 3번까지 다른 결과 반환)
     */
    fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse {
        val response = googlePlacesClient.textSearch(request.query, totalFetchSize)
            ?: return PlacesSearchResponse(emptyList())

        val currentOffset = queryOffsetMap.getOrDefault(request.query, 0)
        val currentCallCount = currentOffset / request.maxResults
        
        if (currentCallCount >= maxCallCount) {
            queryOffsetMap[request.query] = 0
            return PlacesSearchResponse(emptyList())
        }
        
        val startIndex = currentOffset
        val endIndex = minOf(startIndex + request.maxResults, response.results.size)
        
        val selectedResults = if (startIndex < response.results.size) {
            response.results.subList(startIndex, endIndex)
        } else {
            queryOffsetMap[request.query] = 0
            emptyList()
        }

        val items = selectedResults.map { result ->
            val placeDetails = googlePlacesClient.getPlaceDetails(result.placeId)?.result
            
            val topReview = placeDetails?.reviews
                ?.maxByOrNull { it.rating }
                ?.let { review ->
                    PlacesSearchResponse.PlaceItem.Review(
                        rating = review.rating,
                        text = review.text
                    )
                }
            
            PlacesSearchResponse.PlaceItem(
                name = result.name,
                address = result.formattedAddress,
                rating = result.rating,
                userRatingsTotal = result.userRatingsTotal,
                openNow = result.openingHours?.openNow,
                photos = null,
                link = result.url ?: generateGoogleMapsLink(result.placeId),
                website = placeDetails?.website,
                weekdayText = placeDetails?.openingHours?.weekdayText,
                topReview = topReview
            )
        }

        queryOffsetMap[request.query] = endIndex

        return PlacesSearchResponse(items)
    }

    /**
     * Google Maps 장소 링크 생성
     */
    private fun generateGoogleMapsLink(placeId: String): String {
        return "https://www.google.com/maps/place/?q=place_id:$placeId"
    }
}
