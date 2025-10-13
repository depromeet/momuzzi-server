package org.depromeet.team3.place.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.depromeet.team3.place.util.PlaceDetailsProcessor
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
    private val placeDetailsProcessor: PlaceDetailsProcessor
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
        
        // 1. Google Places API 호출 (10개)
        val response = fetchPlacesFromGoogle(queryKey)
        val allPlaces = response.places ?: return@coroutineScope PlacesSearchResponse(emptyList())
        
        // 2. 전체 10개에 대해 Details 조회 및 DB 저장 (배치)
        val allPlaceDetails = placeDetailsProcessor.fetchPlaceDetailsInParallel(allPlaces)
        
        // 3. Offset 관리 - DB에 저장된 전체 결과 중 5개씩 선택
        val selectedDetails = searchPlaceOffsetManager.selectWithOffset(queryKey, request.maxResults, allPlaceDetails)
            ?: return@coroutineScope PlacesSearchResponse(emptyList())
        
        // 4. infra 레이어의 결과를 API 응답 DTO로 변환
        val items = selectedDetails.map { detail ->
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
        if (query.isBlank()) {
            throw PlaceSearchException(
                org.depromeet.team3.common.exception.ErrorCode.PLACE_INVALID_QUERY,
                detail = mapOf("query" to query)
            )
        }
        
        return try {
            withContext(Dispatchers.IO) {
                placeQuery.textSearch(query, totalFetchSize)
            } ?: throw PlaceSearchException(
                org.depromeet.team3.common.exception.ErrorCode.PLACE_API_RESPONSE_NULL,
                detail = mapOf("query" to query)
            )
        } catch (e: PlaceSearchException) {
            throw e
        } catch (e: Exception) {
            logger.error("Google Places API 호출 실패: query=$query", e)
            throw PlaceSearchException(
                org.depromeet.team3.common.exception.ErrorCode.PLACE_SEARCH_FAILED,
                detail = mapOf("query" to query, "error" to e.message)
            )
        }.also { response ->
            if (response.places.isNullOrEmpty()) {
                logger.debug("검색 결과 없음: places is null or empty")
            }
        }
    }
}
