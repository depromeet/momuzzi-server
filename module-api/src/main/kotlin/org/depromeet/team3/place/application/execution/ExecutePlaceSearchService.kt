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
import org.depromeet.team3.place.application.plan.CreateSurveyKeywordService
import org.depromeet.team3.place.application.model.PlaceSearchPlan
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceDetailsProcessor
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * PlaceSearchPlan(Manual/Automatic)에 따라 Google Places API 호출을 실행하고
 * 장소 상세 정보·좋아요·가중치 기반 정렬을 적용해 최종 검색 응답을 생성한다.
 *
 * - Manual: 사용자가 직접 입력한 키워드로 단일 검색
 * - Automatic: 설문 기반으로 생성된 여러 키워드로 병렬 검색 후 가중치 기반 병합
 */
@Service
class ExecutePlaceSearchService(
    private val placeQuery: PlaceQuery,
    private val placeDetailsProcessor: PlaceDetailsProcessor,
    private val meetingPlaceRepository: MeetingPlaceRepository,
    private val placeLikeRepository: PlaceLikeRepository
) {

    private val logger = LoggerFactory.getLogger(ExecutePlaceSearchService::class.java)
    private val totalFetchSize = 15

    suspend fun search(request: PlacesSearchRequest, plan: PlaceSearchPlan): PlacesSearchResponse = supervisorScope {
        val keywordResult = when (plan) {
            // 수동 검색 : 사용자가 입력한 단일 키워드로 검색 (가중치 없음)
            is PlaceSearchPlan.Manual -> {
                val response = fetchPlacesFromGoogle(plan.keyword, plan.stationCoordinates)
                KeywordSearchResult(
                    places = response.places ?: emptyList(),
                    placeWeights = emptyMap(),
                    usedKeywords = listOf(plan.keyword)
                )
            }

            // 자동 검색: 설문 기반 키워드 목록으로 병렬 검색 후 가중치 기반 병합
            is PlaceSearchPlan.Automatic -> {
                fetchPlacesForKeywords(plan)
            }
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
        // 1. 자동 검색(가중치 있음): 가중치 → 좋아요 순
        // 2. 수동 검색(meetingId 있음): 좋아요 순
        // 3. 그 외: API 응답 순서 유지
        val sortedItems = when {
            placeWeightByDbId.isNotEmpty() ->
                items.sortedWith(
                    compareByDescending<PlacesSearchResponse.PlaceItem> { placeWeightByDbId[it.placeId] ?: 0.0 }
                        .thenByDescending { it.likeCount }
                )

            request.meetingId != null ->
                items.sortedByDescending { it.likeCount }

            else -> items
        }

        logger.info(
            "장소 검색 완료 - keywords={}, meetingId={}, 응답수={}",
            keywordResult.usedKeywords,
            request.meetingId,
            sortedItems.size
        )

        PlacesSearchResponse(sortedItems)
    }

    /**
     * 설문 기반 키워드 목록으로 병렬 검색을 수행하고 결과를 가중치 기반으로 병합한다.
     *
     * 처리 흐름:
     * 1. 각 키워드마다 독립적인 코루틴으로 Google Places API 병렬 호출
     * 2. 실패한 키워드는 무시하고 성공한 결과만 수집
     * 3. 모든 장소에 키워드 가중치를 부여하여 수집
     * 4. 중복된 장소는 최고 가중치로 통합
     * 5. 가중치 순으로 정렬하여 반환
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

        // 2단계: 모든 장소를 수집하고 가중치 매핑 (중복 장소는 최고 가중치 유지)
        val placeById = mutableMapOf<String, PlacesTextSearchResponse.Place>()
        val placeWeights = mutableMapOf<String, Double>()

        results.forEach { (candidate, response) ->
            val places = response.places ?: emptyList()
            places.forEach { place ->
                // 장소를 처음 보거나, 이미 본 장소라도 가중치 정보는 업데이트
                placeById.putIfAbsent(place.id, place)
                
                // 가중치는 여러 키워드에서 나온 경우 최댓값 유지
                val currentWeight = placeWeights[place.id] ?: 0.0
                if (candidate.weight > currentWeight) {
                    placeWeights[place.id] = candidate.weight
                }
            }
        }

        // 3단계: 가중치 순으로 정렬
        val sortedPlaces = placeById.values
            .sortedByDescending { placeWeights[it.id] ?: 0.0 }

        KeywordSearchResult(
            places = sortedPlaces,
            placeWeights = placeWeights,
            usedKeywords = plan.keywords.map { it.keyword }
        )
    }

    /**
     * Google Places Text Search API를 호출하여 장소 목록을 조회한다.
     *
     * @param query 검색 키워드 (이미 정규화된 상태로 전달됨)
     * @param stationCoordinates 역 좌표 (있으면 반경 3km 내 우선 검색)
     * @return Google Places API 응답 (최대 15개 장소)
     */
    private suspend fun fetchPlacesFromGoogle(
        query: String,
        stationCoordinates: MeetingQuery.StationCoordinates?
    ): PlacesTextSearchResponse {
        val normalizedQuery = CreateSurveyKeywordService.normalizeKeyword(query)
        if (normalizedQuery.isBlank()) {
            throw PlaceSearchException(
                ErrorCode.PLACE_INVALID_QUERY,
                detail = mapOf("query" to query)
            )
        }

        return try {
            withContext(Dispatchers.IO) {
                placeQuery.textSearch(
                    query = normalizedQuery,
                    maxResults = totalFetchSize,
                    latitude = stationCoordinates?.latitude,
                    longitude = stationCoordinates?.longitude,
                    radius = 3000.0  // 역 중심 반경 3km 내 우선 검색
                )
            }
        } catch (e: PlaceSearchException) {
            throw e
        } catch (e: Exception) {
            logger.error("Google Places API 호출 실패: query=$normalizedQuery", e)
            throw PlaceSearchException(
                ErrorCode.PLACE_SEARCH_FAILED,
                detail = mapOf("query" to normalizedQuery, "error" to e.message)
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