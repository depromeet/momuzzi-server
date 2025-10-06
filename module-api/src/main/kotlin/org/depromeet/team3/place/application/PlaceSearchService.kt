package org.depromeet.team3.place.application

import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesSearchResponse as GooglePlacesSearchResponse
import org.springframework.stereotype.Service
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .maximumSize(500)
        .build<String, Int>()
    
    /**
     * 검색어별 Mutex를 관리하는 맵
     */
    private val queryMutexMap = ConcurrentHashMap<String, Mutex>()

    private val totalFetchSize = 10
    private val maxCallCount = 2

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
        val selectedResults = selectResultsWithOffset(queryKey, request.maxResults, response) 
            ?: return@coroutineScope PlacesSearchResponse(emptyList())
        
        // 3. 상세 정보 병렬 조회
        val items = fetchPlaceDetailsInParallel(selectedResults)
        
        PlacesSearchResponse(items)
    }

    /**
     * 1. Google Places API 호출
     */
    private suspend fun fetchPlacesFromGoogle(query: String): GooglePlacesSearchResponse {
        return try {
            withContext(Dispatchers.IO) {
                googlePlacesClient.textSearch(query, totalFetchSize)
            } ?: throw PlaceSearchException("Google Places API 응답이 null입니다")
        } catch (e: Exception) {
            logger.error("Google Places API 호출 실패: query=$query", e)
            throw PlaceSearchException("맛집 검색 중 오류가 발생했습니다", e)
        }.also { response ->
            if (response.status != "OK") {
                logger.warn("Google Places API 비정상 응답: status=${response.status}")
                throw PlaceSearchException("Google Places API 응답 상태: ${response.status}")
            }
        }
    }

    /**
     * 2. Offset 관리 + Mutex 보호
     * 
     * 같은 query에 대한 동시 요청을 Mutex로 직렬화하여
     * offset을 원자적으로 읽고 갱신
     */
    private suspend fun selectResultsWithOffset(
        queryKey: String,
        maxResults: Int,
        response: GooglePlacesSearchResponse
    ): List<GooglePlacesSearchResponse.PlaceResult>? {
        val mutex = queryMutexMap.computeIfAbsent(queryKey) { Mutex() }
        
        var startIndex = 0
        var shouldReturnEmpty = false
        
        try {
            mutex.withLock {
                val currentOffset = queryOffsetCache.getIfPresent(queryKey) ?: 0
                val currentCallCount = currentOffset / maxResults
                
                when {
                    currentCallCount >= maxCallCount -> {
                        logger.debug("검색어의 최대 호출 횟수 도달: query=$queryKey")
                        queryOffsetCache.invalidate(queryKey)
                        shouldReturnEmpty = true
                    }
                    currentOffset >= response.results.size -> {
                        logger.debug("검색어의 offset이 결과 크기 초과: query=$queryKey")
                        queryOffsetCache.invalidate(queryKey)
                        shouldReturnEmpty = true
                    }
                    else -> {
                        startIndex = currentOffset
                        queryOffsetCache.put(queryKey, currentOffset + maxResults)
                        logger.debug("검색어 offset 갱신: query=$queryKey, offset: $currentOffset -> ${currentOffset + maxResults}")
                    }
                }
            }
        } finally {
            // 캐시가 만료되면 mutex도 제거 (메모리 누수 방지)
            if (queryOffsetCache.getIfPresent(queryKey) == null) {
                queryMutexMap.remove(queryKey)
            }
        }
        
        if (shouldReturnEmpty) {
            return null
        }
        
        val endIndex = minOf(startIndex + maxResults, response.results.size)
        return response.results.subList(startIndex, endIndex)
    }

    /**
     * 3. 상세 정보 병렬 조회
     * 여러 장소의 상세 정보를 동시에 병렬로 가져옵니다.
     */
    private suspend fun fetchPlaceDetailsInParallel(
        results: List<GooglePlacesSearchResponse.PlaceResult>
    ): List<PlacesSearchResponse.PlaceItem> = coroutineScope {
        results.map { result ->
            async(Dispatchers.IO) {
                try {
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
                } catch (e: Exception) {
                    logger.warn("장소 상세 정보 조회 실패: place=${result.name}", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()
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
