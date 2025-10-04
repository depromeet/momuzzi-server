package org.depromeet.team3.place.application

import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class PlaceSearchService(
    private val googlePlacesClient: GooglePlacesClient,
    private val googlePlacesApiProperties: org.depromeet.team3.common.GooglePlacesApiProperties
) {
    // 검색어별 offset 관리 (메모리 캐시)
    private val queryOffsetMap = ConcurrentHashMap<String, Int>()
    
    // Google API에서 가져올 전체 결과 개수 (3번 호출 * 5개 = 15개)
    private val totalFetchSize = 15
    
    // 최대 호출 횟수
    private val maxCallCount = 3

    fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse {
        val response = googlePlacesClient.textSearch(request.query, totalFetchSize)
            ?: return PlacesSearchResponse(emptyList())

        // 현재 offset 가져오기
        val currentOffset = queryOffsetMap.getOrDefault(request.query, 0)
        val currentCallCount = currentOffset / request.maxResults
        
        // 3번 호출 제한 체크
        if (currentCallCount >= maxCallCount) {
            queryOffsetMap[request.query] = 0
            return PlacesSearchResponse(emptyList())
        }
        
        // 결과에서 현재 offset부터 maxResults개만큼 추출
        val startIndex = currentOffset
        val endIndex = minOf(startIndex + request.maxResults, response.results.size)
        
        val selectedResults = if (startIndex < response.results.size) {
            response.results.subList(startIndex, endIndex)
        } else {
            // offset이 결과 범위를 벗어나면 빈 결과 반환
            queryOffsetMap[request.query] = 0
            emptyList()
        }

        val items = selectedResults.map { result ->
            // Place Details로 전체 정보 가져오기
            val placeDetails = googlePlacesClient.getPlaceDetails(result.placeId)?.result
            
            // 가장 높은 평점의 리뷰 하나만 선택
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

        // 다음 호출을 위해 offset 업데이트
        queryOffsetMap[request.query] = endIndex

        return PlacesSearchResponse(items)
    }

    private fun generateGoogleMapsLink(placeId: String): String {
        return "https://www.google.com/maps/place/?q=place_id:$placeId"
    }

    private fun generatePhotoUrl(photoReference: String): String {
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=$photoReference&key=${googlePlacesApiProperties.apiKey}"
    }
}
