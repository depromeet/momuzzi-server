package org.depromeet.team3.place.application.execution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingQuery
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.application.model.PlaceSearchPlan
import org.depromeet.team3.place.application.plan.CreateSurveyKeywordService
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceDetailsProcessor
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.ln

/**
 * PlaceSearchPlan(Manual/Automatic)에 따라 Google Places API 호출을 실행하고
 * 장소 상세 정보·좋아요·가중치 기반 정렬을 적용해 최종 검색 응답을 생성한다.
 *
 * - Automatic: 설문 기반으로 생성된 여러 키워드로 병렬 검색 후 가중치 기반 병합
 */
@Service
class ExecutePlaceSearchService(
    private val placeQuery: PlaceQuery,
    private val placeDetailsProcessor: PlaceDetailsProcessor,
    private val meetingPlaceRepository: MeetingPlaceRepository,
    private val placeLikeRepository: PlaceLikeRepository,
    private val cacheManager: MeetingPlaceSearchCacheManager
) {

    private val logger = LoggerFactory.getLogger(ExecutePlaceSearchService::class.java)
    private val totalFetchSize = 10  // 최종 반환 개수
    private val weightScoreMultiplier = 100.0
    private val likeScoreMultiplier = 15.0

    suspend fun search(request: PlacesSearchRequest, plan: PlaceSearchPlan): PlacesSearchResponse = supervisorScope {
        // 캐시 확인 (Automatic 검색 + meetingId 있을 때만)
        val cachedResult = if (plan is PlaceSearchPlan.Automatic && request.meetingId != null) {
            cacheManager.getCachedAutomaticResult(request.meetingId)
        } else null
        
        val keywordResult = if (cachedResult != null) {
            // 캐시 HIT: 저장된 결과를 그대로 사용 (Place 정보 + 가중치)
            logger.info("자동 검색 캐시 HIT - meetingId=${request.meetingId}, places=${cachedResult.placeIds.size}개")
            val places = fetchPlacesFromCache(cachedResult.placeIds)
            KeywordSearchResult(
                places = places,
                placeWeights = cachedResult.placeWeights,
                usedKeywords = cachedResult.usedKeywords
            )
        } else {
            val automaticPlan = plan as? PlaceSearchPlan.Automatic
                ?: throw IllegalArgumentException("PlaceSearchPlan.Automatic만 지원합니다.")

            val result = fetchPlacesForKeywords(automaticPlan)

            if (request.meetingId != null && result.places.isNotEmpty()) {
                cacheManager.cacheAutomaticResult(
                    meetingId = request.meetingId,
                    placeIds = result.places.map { it.id },
                    placeWeights = result.placeWeights,
                    usedKeywords = result.usedKeywords
                )
            }

            result
        }

        if (keywordResult.places.isEmpty()) {
            logger.info("장소 검색 결과 없음 - keywords={}, meetingId={}", keywordResult.usedKeywords, request.meetingId)
            return@supervisorScope PlacesSearchResponse(emptyList())
        }

        // 가중치 기반으로 정렬 후 상위 15개만 상세 조회
        val placesToProcess = keywordResult.places
            .sortedByDescending { keywordResult.placeWeights[it.id] ?: 0.0 }
            .take(totalFetchSize)
        val allPlaceDetails = placeDetailsProcessor.fetchPlaceDetailsInParallel(placesToProcess)

        if (allPlaceDetails.isEmpty()) {
            logger.info("PlaceDetails 결과 없음 - keywords={}, meetingId={}", keywordResult.usedKeywords, request.meetingId)
            return@supervisorScope PlacesSearchResponse(emptyList())
        }

        val meetingPlaces = if (request.meetingId != null) {
            val placeDbIds = getPlaceDbIds(allPlaceDetails.map { it.placeId })
            createOrGetMeetingPlaces(request.meetingId, placeDbIds)
        } else {
            emptyList()
        }

        val googlePlaceIds = allPlaceDetails.map { it.placeId }
        val placeIdMap = getPlaceStringIdToDbIdMap(googlePlaceIds)
        val placeWeightByDbId = keywordResult.placeWeights.mapNotNull { (googleId, weight) ->
            placeIdMap[googleId]?.let { it to weight }
        }.toMap()

        val likesMap = if (meetingPlaces.isNotEmpty()) {
            buildLikesMap(googlePlaceIds, meetingPlaces, request.userId)
        } else {
            emptyMap()
        }

        val items = allPlaceDetails.mapNotNull { detail ->
            runCatching {
                val placeDbId = placeIdMap[detail.placeId]
                    ?: throw IllegalStateException("DB Place ID 없음: googlePlaceId=${detail.placeId}, name=${detail.name}")
                val likeInfo = likesMap[detail.placeId] ?: PlaceLikeInfo(0, false)

                PlacesSearchResponse.PlaceItem(
                    placeId = placeDbId,
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
                        PlacesSearchResponse.PlaceItem.AddressDescriptor(description = desc)
                    },
                    likeCount = likeInfo.likeCount,
                    isLiked = likeInfo.isLiked
                )
            }.onFailure { e ->
                logger.warn("장소 응답 변환 실패: placeId=${detail.placeId}, error=${e.message}")
            }.getOrNull()
        }

        // 정렬 우선순위:
        // 자동 검색(가중치 있음): (설문가중치 * 100) + (ln(좋아요+1) * 10) 점수 → 좋아요 순
        val sortedItems = when {
            placeWeightByDbId.isNotEmpty() -> {
                val scoreByPlaceId = items.associate { item ->
                    val weight = placeWeightByDbId[item.placeId] ?: 0.0
                    val likeScore = if (item.likeCount > 0) ln(item.likeCount.toDouble() + 1) * likeScoreMultiplier else 0.0
                    val combinedScore = weight * weightScoreMultiplier + likeScore
                    item.placeId to combinedScore
                }

                items.sortedWith(
                    compareByDescending<PlacesSearchResponse.PlaceItem> { scoreByPlaceId[it.placeId] ?: 0.0 }
                        .thenByDescending { it.likeCount }
                )
            }

            else -> items
        }

        PlacesSearchResponse(sortedItems)
    }

    /**
     * 캐시된 Google Place ID 리스트로 DB에서 Place 정보 조회
     * (Google API 호출 없이 DB 캐시만 사용)
     */
    private suspend fun fetchPlacesFromCache(
        googlePlaceIds: List<String>
    ): List<PlacesTextSearchResponse.Place> {
        return withContext(Dispatchers.IO) {
            val places = placeQuery.findByGooglePlaceIds(googlePlaceIds)
            val placeMap = places.mapNotNull { place ->
                val gId = place.googlePlaceId ?: return@mapNotNull null
                gId to place
            }.toMap()
            
            // 캐시된 순서대로 Place 생성
            googlePlaceIds.mapNotNull { googlePlaceId ->
                val place = placeMap[googlePlaceId]
                if (place == null) {
                    logger.warn("캐시된 Place를 DB에서 찾을 수 없음: googlePlaceId=$googlePlaceId")
                    return@mapNotNull null
                }
                
                // PlaceEntity -> PlacesTextSearchResponse.Place 변환
                PlacesTextSearchResponse.Place(
                    id = place.googlePlaceId ?: googlePlaceId,
                    displayName = PlacesTextSearchResponse.Place.DisplayName(text = place.name),
                    formattedAddress = place.address,
                    rating = place.rating,
                    userRatingCount = place.userRatingsTotal,
                    currentOpeningHours = if (place.openNow != null) {
                        PlacesTextSearchResponse.Place.OpeningHours(openNow = place.openNow)
                    } else null
                )
            }
        }
    }
    
    /**
     * 설문 기반 키워드 목록으로 병렬 검색을 수행하고 결과를 가중치 비례로 병합한다.
     *
     * 처리 흐름:
     * 1. 각 키워드마다 독립적인 코루틴으로 Google Places API 병렬 호출
     * 2. 총 10개를 가중치 비례로 키워드별 할당량 계산
     * 3. 각 키워드에서 할당량만큼 평점 높은 장소 선택
     * 4. 최종 결과를 가중치 순으로 정렬 (같은 가중치면 평점 순)
     *
     * 예시:
     * - 한식 40%, 양식 30%, 일식 20%, 중식 10%
     * - → 한식 4개, 양식 3개, 일식 2개, 중식 1개
     *
     * 정렬 우선순위:
     * 1순위: 가중치 (설문 득표율) - 높을수록 먼저
     * 2순위: 좋아요 개수 (좋아요 정보는 나중에 DB에서 조회하여 적용)
     *
     * @param plan 설문 기반으로 생성된 키워드 목록과 역 좌표
     * @return 병합된 장소 목록, 장소별 가중치, 사용된 키워드 목록
     */
    private suspend fun fetchPlacesForKeywords(plan: PlaceSearchPlan.Automatic): KeywordSearchResult = coroutineScope {
        // 1단계: 각 키워드로 병렬 API 호출
        val deferredResponses = plan.keywords.map { candidate ->
            async {
                runCatching {
                    candidate to fetchPlacesFromGoogle(candidate.keyword, plan.stationCoordinates)
                }.onFailure { e ->
                    logger.warn("Google Places API 호출 실패(자동 키워드): keyword={}, message={}", candidate.keyword, e.message)
                }.getOrNull()
            }
        }

        val results = deferredResponses.awaitAll().filterNotNull()

        // 2단계: 가중치 비례로 각 키워드별 할당량 계산
        val allocations = calculateKeywordAllocations(
            results.map { it.first.weight },
            totalFetchSize
        )

        // 3단계: 각 키워드에서 할당량만큼 선택
        val selectedPlaces = mutableListOf<PlacesTextSearchResponse.Place>()
        val placeWeights = mutableMapOf<String, Double>()
        val usedPlaceIds = mutableSetOf<String>()
        val appliedKeywords = mutableSetOf<String>()

        results.forEach { appliedKeywords.add(it.first.keyword) }

        results.forEachIndexed { index, (candidate, response) ->
            val allocation = allocations[index]
            if (allocation == 0) return@forEachIndexed
            
            val rawPlaces = response.places ?: emptyList()
            val places = filterPlacesByKeyword(rawPlaces, candidate)
            var addedCount = 0
            
            // 평점 높은 순으로 정렬하여 할당량만큼 선택
            places
                .filter { !usedPlaceIds.contains(it.id) }  // 중복 제거
                .sortedByDescending { it.rating ?: 0.0 }   // 평점 순
                .take(allocation)                           // 할당량만큼
                .forEach { place ->
                    selectedPlaces.add(place)
                    placeWeights[place.id] = candidate.weight
                    usedPlaceIds.add(place.id)
                    addedCount++
                }

            if (addedCount < allocation) {
                val fallbackKeyword = candidate.fallbackKeyword
                if (!fallbackKeyword.isNullOrBlank()) {
                    val fallbackResponse = fetchPlacesFromGoogle(fallbackKeyword, plan.stationCoordinates)
                    val fallbackRawPlaces = fallbackResponse.places ?: emptyList()
                    val fallbackPlaces = filterPlacesByKeyword(fallbackRawPlaces, candidate, candidate.fallbackMatchKeywords)
                    var fallbackAdded = false
                    fallbackPlaces
                        .filter { !usedPlaceIds.contains(it.id) }
                        .sortedByDescending { it.rating ?: 0.0 }
                        .take(allocation - addedCount)
                        .forEach { place ->
                            selectedPlaces.add(place)
                            placeWeights[place.id] = candidate.weight
                            usedPlaceIds.add(place.id)
                            addedCount++
                            fallbackAdded = true
                        }

                    if (fallbackAdded) {
                        appliedKeywords.add(fallbackKeyword)
                    }
                }
            }
        }

        // 4단계: 할당량을 못 채운 경우, 가중치 높은 키워드부터 추가 선택
        if (selectedPlaces.size < totalFetchSize) {
            val sortedResults = results.sortedByDescending { it.first.weight }
            
            sortedResults.forEach { (candidate, response) ->
                if (selectedPlaces.size >= totalFetchSize) return@forEach
                
                val places = filterPlacesByKeyword(response.places ?: emptyList(), candidate)
                places
                    .filter { !usedPlaceIds.contains(it.id) }
                    .sortedByDescending { it.rating ?: 0.0 }
                    .take(totalFetchSize - selectedPlaces.size)
                    .forEach { place ->
                        selectedPlaces.add(place)
                        placeWeights[place.id] = candidate.weight
                        usedPlaceIds.add(place.id)
                    }
            }
        }

        // 5단계: 최종 정렬 (가중치 순 → 평점 순)
        // 좋아요 정렬은 나중에 search() 메서드에서 DB 데이터와 함께 처리됨
        val sortedPlaces = selectedPlaces
            .sortedWith(
                compareByDescending<PlacesTextSearchResponse.Place> { placeWeights[it.id] ?: 0.0 }
                    .thenByDescending { it.rating ?: 0.0 }
            )
            .take(totalFetchSize)

        if (sortedPlaces.isNotEmpty()) {
            return@coroutineScope KeywordSearchResult(
                places = sortedPlaces,
                placeWeights = placeWeights,
                usedKeywords = appliedKeywords.toList()
            )
        }

        logger.warn("설문 기반 키워드로 결과 없음 - fallback keyword 사용: {}", plan.fallbackKeyword)

        val fallbackResponse = fetchPlacesFromGoogle(plan.fallbackKeyword, plan.stationCoordinates)
        val fallbackPlaces = (fallbackResponse.places ?: emptyList()).take(totalFetchSize)
        val fallbackWeights = fallbackPlaces.associate { it.id to 0.1 }

        KeywordSearchResult(
            places = fallbackPlaces,
            placeWeights = fallbackWeights,
            usedKeywords = listOf(plan.fallbackKeyword)
        )
    }

    private fun filterPlacesByKeyword(
        places: List<PlacesTextSearchResponse.Place>,
        candidate: CreateSurveyKeywordService.KeywordCandidate,
        overrideKeywords: Set<String>? = null
    ): List<PlacesTextSearchResponse.Place> {
        val keywords = overrideKeywords ?: candidate.matchKeywords
        if (keywords.isEmpty()) {
            return emptyList()
        }

        val filtered = places.filter { place ->
            val normalizedName = place.displayName.text.lowercase().replace(" ", "")
            val types = place.types?.map { it.lowercase() } ?: emptyList()

            keywords.any { keyword ->
                normalizedName.contains(keyword) ||
                    place.displayName.text.lowercase().contains(keyword) ||
                    types.any { it.contains(keyword) }
            }
        }

        return if (filtered.isNotEmpty()) filtered else emptyList()
    }

    /**
     * 가중치 비례로 각 키워드별 할당량 계산
     * 
     * @param weights 각 키워드의 가중치 리스트
     * @param totalSlots 총 할당할 개수
     * @return 각 키워드별 할당량 리스트
     */
    private fun calculateKeywordAllocations(weights: List<Double>, totalSlots: Int): List<Int> {
        if (weights.isEmpty()) return emptyList()
        
        val totalWeight = weights.sum()
        if (totalWeight == 0.0) {
            // 모든 가중치가 0이면 균등 분배
            val equalSlots = totalSlots / weights.size
            return List(weights.size) { equalSlots }
        }
        
        // 가중치 비례로 계산
        val allocations = weights.map { weight ->
            ((weight / totalWeight) * totalSlots).toInt()
        }.toMutableList()
        
        // 반올림 오차로 인한 부족분 처리
        var allocated = allocations.sum()
        if (allocated < totalSlots) {
            // 가중치 높은 순서대로 1개씩 추가
            val sortedIndices = weights.indices.sortedByDescending { weights[it] }
            for (i in 0 until (totalSlots - allocated)) {
                allocations[sortedIndices[i % sortedIndices.size]]++
            }
        }
        
        logger.debug(
            "키워드 할당량 계산 - 가중치: {}, 할당: {}, 총: {}개",
            weights.map { "%.2f".format(it) },
            allocations,
            allocations.sum()
        )
        
        return allocations
    }

    /**
     * Google Places Text Search API를 호출하여 장소 목록을 조회한다.
     *
     * @param query 검색 키워드 (호출 시점에서 정규화되어 있지만, 방어적으로 한 번 더 정규화)
     * @param stationCoordinates 역 좌표 (있으면 반경 3km 내 우선 검색)
     * @return Google Places API 응답 (최대 15개 장소)
     */
    private suspend fun fetchPlacesFromGoogle(
        query: String,
        stationCoordinates: MeetingQuery.StationCoordinates?
    ): PlacesTextSearchResponse {
        // 호출부에서 정규화되지만, 외부 입력을 고려해 한 번 더 정규화
        val sanitizedQuery = CreateSurveyKeywordService.normalizeKeyword(query)
        if (sanitizedQuery.isBlank()) {
            throw PlaceSearchException(
                ErrorCode.PLACE_INVALID_QUERY,
                detail = mapOf("query" to query)
            )
        }

        return try {
            withContext(Dispatchers.IO) {
                placeQuery.textSearch(
                    query = sanitizedQuery,
                    maxResults = totalFetchSize,
                    latitude = stationCoordinates?.latitude,
                    longitude = stationCoordinates?.longitude,
                    radius = 3000.0  // 역 중심 반경 3km 내 우선 검색
                )
            }
        } catch (e: PlaceSearchException) {
            throw e
        } catch (e: Exception) {
            logger.error("Google Places API 호출 실패: query=$sanitizedQuery", e)
            throw PlaceSearchException(
                ErrorCode.PLACE_SEARCH_FAILED,
                detail = mapOf("query" to sanitizedQuery, "error" to e.message)
            )
        }
    }

    private suspend fun getPlaceDbIds(googlePlaceIds: List<String>): List<Long> {
        return withContext(Dispatchers.IO) {
            placeQuery.findByGooglePlaceIds(googlePlaceIds).mapNotNull { it.id }
        }
    }

    private suspend fun getPlaceStringIdToDbIdMap(googlePlaceIds: List<String>): Map<String, Long> {
        return withContext(Dispatchers.IO) {
            placeQuery.findByGooglePlaceIds(googlePlaceIds)
                .mapNotNull { place ->
                    val dbId = place.id ?: return@mapNotNull null
                    val gId = place.googlePlaceId ?: return@mapNotNull null
                    gId to dbId
                }
                .toMap()
        }
    }

    private suspend fun createOrGetMeetingPlaces(meetingId: Long, placeDbIds: List<Long>): List<MeetingPlace> {
        val existingMeetingPlaces = meetingPlaceRepository.findByMeetingId(meetingId)
        val existingPlaceIds = existingMeetingPlaces.map { it.placeId }.toSet()

        val newMeetingPlaces = placeDbIds
            .filter { it !in existingPlaceIds }
            .map { placeDbId ->
                MeetingPlace(
                    meetingId = meetingId,
                    placeId = placeDbId
                )
            }

        return if (newMeetingPlaces.isNotEmpty()) {
            val saved = meetingPlaceRepository.saveAll(newMeetingPlaces)
            existingMeetingPlaces + saved
        } else {
            existingMeetingPlaces
        }
    }

    private suspend fun buildLikesMap(
        googlePlaceIds: List<String>,
        meetingPlaces: List<MeetingPlace>,
        userId: Long?
    ): Map<String, PlaceLikeInfo> {
        val placeStringIdToDbId = getPlaceStringIdToDbIdMap(googlePlaceIds)
        val meetingPlaceIds = meetingPlaces.mapNotNull { it.id }

        if (meetingPlaceIds.isEmpty()) {
            return emptyMap()
        }

        val placeLikes = placeLikeRepository.findByMeetingPlaceIds(meetingPlaceIds)

        val meetingPlaceIdToPlaceDbId = meetingPlaces
            .filter { it.id != null }
            .associate { it.id!! to it.placeId }

        val likesByPlaceDbId = placeLikes
            .groupBy { meetingPlaceIdToPlaceDbId[it.meetingPlaceId] }

        return googlePlaceIds.associateWith { googlePlaceId ->
            val placeDbId = placeStringIdToDbId[googlePlaceId]
            val likes = if (placeDbId != null) likesByPlaceDbId[placeDbId] ?: emptyList() else emptyList()

            PlaceLikeInfo(
                likeCount = likes.size,
                isLiked = userId != null && likes.any { it.userId == userId }
            )
        }
    }

    private data class KeywordSearchResult(
        val places: List<PlacesTextSearchResponse.Place>,
        val placeWeights: Map<String, Double>,
        val usedKeywords: List<String>
    )

    private data class PlaceLikeInfo(
        val likeCount: Int,
        val isLiked: Boolean
    )
}