package org.depromeet.team3.place.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.depromeet.team3.place.util.PlaceDetailsAssembler
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 맛집 검색 서비스 - Orchestrator
 * 전체 검색 프로세스를 조율하고 각 책임을 위임
 */
@Service
class SearchPlacesService(
    private val placeQuery: PlaceQuery,
    private val searchPlaceOffsetManager: SearchPlaceOffsetManager,
    private val placeDetailsAssembler: PlaceDetailsAssembler
) {
    private val logger = LoggerFactory.getLogger(SearchPlacesService::class.java)
    
    private val totalFetchSize = 10

    /**
     * 맛집 검색 및 순차 결과 반환
     *
     * @param request 검색 요청 (검색어, 결과 개수)
     * @return 검색 결과 목록
     */
    suspend fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse = coroutineScope {
        val queryKey = request.query.trim().lowercase()
        
        // 1. Google Places API 호출
        val response = fetchPlacesFromGoogle(queryKey)
        
        // 2. Offset 관리 + Mutex 보호
        val places = response.places ?: return@coroutineScope PlacesSearchResponse(emptyList())
        val selectedPlaces = searchPlaceOffsetManager.selectWithOffset(queryKey, request.maxResults, places)
            ?: return@coroutineScope PlacesSearchResponse(emptyList())
        
        // 3. 상세 정보 병렬 조회 및 변환
        val placeDetails = placeDetailsAssembler.fetchPlaceDetailsInParallel(selectedPlaces)
        
        // 4. infra 레이어의 결과를 API 응답 DTO로 변환
        val items = placeDetails.map { detail ->
            PlacesSearchResponse.PlaceItem(
                name = detail.name,
                address = detail.address,
                rating = detail.rating,
                userRatingsTotal = detail.userRatingsTotal,
                openNow = detail.openNow,
                photos = detail.photos,
                link = detail.link,
                weekdayText = detail.weekdayText,
                topReview = detail.topReview?.let { review ->
                    PlacesSearchResponse.PlaceItem.Review(
                        rating = review.rating,
                        text = review.text
                    )
                },
                priceRange = detail.priceRange?.let { priceRange ->
                    PlacesSearchResponse.PlaceItem.PriceRange(
                        startPrice = priceRange.startPrice,
                        endPrice = priceRange.endPrice
                    )
                },
                addressDescriptor = detail.addressDescriptor?.let { desc ->
                    PlacesSearchResponse.PlaceItem.AddressDescriptor(
                        description = desc
                    )
                }
            )
        }
        
        PlacesSearchResponse(items)
    }

    /**
     * Google Places API 호출
     */
    private suspend fun fetchPlacesFromGoogle(query: String): PlacesTextSearchResponse {
        return try {
            withContext(Dispatchers.IO) {
                placeQuery.textSearch(query, totalFetchSize)
            } ?: throw PlaceSearchException("Google Places API 응답이 null입니다")
        } catch (e: Exception) {
            logger.error("Google Places API 호출 실패: query=$query", e)
            throw PlaceSearchException("맛집 검색 중 오류가 발생했습니다", e)
        }.also { response ->
            if (response.places.isNullOrEmpty()) {
                logger.debug("검색 결과 없음: places is null or empty")
            }
        }
    }
}
