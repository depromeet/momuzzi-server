package org.depromeet.team3.place.application

import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.springframework.stereotype.Service
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Service
class SearchPlacesService(
    private val placeQuery: PlaceQuery,
    private val googlePlacesApiProperties: GooglePlacesApiProperties,
) {
    private val logger = LoggerFactory.getLogger(SearchPlacesService::class.java)
    
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

    /**
     * 2. Offset 관리 + Mutex 보호
     * 
     * 같은 query에 대한 동시 요청을 Mutex로 직렬화하여
     * offset을 원자적으로 읽고 갱신
     */
    private suspend fun selectResultsWithOffset(
        queryKey: String,
        maxResults: Int,
        response: PlacesTextSearchResponse
    ): List<PlacesTextSearchResponse.Place>? {
        val places = response.places ?: return null
        
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
                    currentOffset >= places.size -> {
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
        
        val endIndex = minOf(startIndex + maxResults, places.size)
        return places.subList(startIndex, endIndex)
    }

    /**
     * 3. 상세 정보 병렬 조회
     * 여러 장소의 상세 정보를 동시에 병렬로 가져옵니다.
     */
    private suspend fun fetchPlaceDetailsInParallel(
        results: List<PlacesTextSearchResponse.Place>
    ): List<PlacesSearchResponse.PlaceItem> = coroutineScope {
        results.map { result ->
            async(Dispatchers.IO) {
                try {
                    val placeDetails = placeQuery.getPlaceDetails(result.id)
                    
                    val topReview = placeDetails?.reviews
                        ?.maxByOrNull { it.rating }
                        ?.let { review ->
                            PlacesSearchResponse.PlaceItem.Review(
                                rating = review.rating.toInt(),
                                text = review.text.text
                            )
                        }
                    
                    val photos = placeDetails?.photos?.take(5)?.map { photo ->
                        generatePhotoUrl(photo.name)
                    }
                    
                    val priceRange = placeDetails?.priceRange?.let { range ->
                        PlacesSearchResponse.PlaceItem.PriceRange(
                            startPrice = formatMoney(range.startPrice),
                            endPrice = formatMoney(range.endPrice)
                        )
                    }

                    val addressDescriptor = try {
                        placeDetails?.addressDescriptor?.let { descriptor ->
                            // 1. addressDescriptor에서 역 정보 먼저 찾기
                            val stationLandmark = descriptor.landmarks
                                ?.filter { landmark ->
                                    val types = landmark.types ?: emptyList()
                                    types.contains("transit_station") ||
                                    types.contains("subway_station") ||
                                    types.contains("train_station") ||
                                    landmark.displayName?.text?.endsWith("역") == true
                                }
                                ?.minByOrNull { it.straightLineDistanceMeters ?: Double.MAX_VALUE }
                            
                            if (stationLandmark != null) {
                                // 역 정보가 있으면 바로 사용
                                val distance = stationLandmark.straightLineDistanceMeters?.toInt()
                                val name = stationLandmark.displayName?.text
                                if (distance != null && name != null) {
                                    val walkingMinutes = maxOf(1, distance / 67)
                                    PlacesSearchResponse.PlaceItem.AddressDescriptor(
                                        description = "${name} 도보 약 ${walkingMinutes}분"
                                    )
                                } else null
                            } else {
                                // 2. 역 정보가 없으면 Nearby Search API 호출 (fallback)
                                placeDetails.location?.let { location ->
                                    val nearbyStation = placeQuery.searchNearbyStation(
                                        latitude = location.latitude,
                                        longitude = location.longitude
                                    )
                                    nearbyStation?.let { station ->
                                        val walkingMinutes = maxOf(1, station.distance / 67)
                                        PlacesSearchResponse.PlaceItem.AddressDescriptor(
                                            description = "${station.name} 도보 약 ${walkingMinutes}분"
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("addressDescriptor 처리 실패: place=${result.displayName?.text}", e)
                        null
                    }

                    val koreanName = extractKoreanName(result.displayName.text)
                    
                    PlacesSearchResponse.PlaceItem(
                        name = koreanName,
                        address = result.formattedAddress,
                        rating = result.rating,
                        userRatingsTotal = result.userRatingCount,
                        openNow = result.currentOpeningHours?.openNow,
                        photos = photos,
                        link = generateNaverPlaceLink(koreanName),
                        weekdayText = placeDetails?.regularOpeningHours?.weekdayDescriptions,
                        topReview = topReview,
                        priceRange = priceRange,
                        addressDescriptor = addressDescriptor
                    )
                } catch (e: Exception) {
                    logger.warn("장소 상세 정보 조회 실패: place=${result.displayName.text}", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    /**
     * 네이버 플레이스 링크 생성
     */
    private fun generateNaverPlaceLink(placeName: String): String {
        return "https://m.place.naver.com/place/list?query=$placeName"
    }

    /**
     * Google Places Photo URL 생성 (New API)
     * Photo name 형식: places/{place_id}/photos/{photo_reference}
     */
    private fun generatePhotoUrl(photoName: String): String {
        return "https://places.googleapis.com/v1/${photoName}/media?maxHeightPx=400&maxWidthPx=400&key=${googlePlacesApiProperties.apiKey}"
    }
    
    /**
     * Money 객체를 문자열로 포맷
     */
    private fun formatMoney(money: org.depromeet.team3.place.model.PlaceDetailsResponse.PriceRange.Money?): String? {
        if (money == null) return null
        val amount = money.units ?: "0"
        return "${money.currencyCode} $amount"
    }
    
    /**
     * 장소 이름에서 한국어 부분만 추출
     * 예: "바비레드 강남본점 Korean-Italian Fusion Restaurant 韓伊フュージョンレストラン 韩意融合餐厅" 
     *     -> "바비레드 강남본점"
     */
    private fun extractKoreanName(fullName: String): String {
        // 한글, 숫자, 공백, 일부 특수문자만 추출
        val koreanPattern = Regex("[가-힣0-9\\s\\-()]+")
        val matches = koreanPattern.findAll(fullName)
        
        return matches
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .firstOrNull()
            ?.trim()
            ?: fullName // 한국어가 없으면 원본 반환
    }
}
