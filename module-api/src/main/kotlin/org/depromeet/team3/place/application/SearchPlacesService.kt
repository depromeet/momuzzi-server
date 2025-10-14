package org.depromeet.team3.place.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.place.util.PlaceDetailsProcessor
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 맛집 검색 서비스 - Orchestrator
 * 전체 검색 프로세스를 조율하고 각 책임을 위임
 */
@Service
class SearchPlacesService(
    private val placeQuery: PlaceQuery,
    private val placeDetailsProcessor: PlaceDetailsProcessor,
    private val meetingPlaceRepository: MeetingPlaceRepository,
    private val placeLikeRepository: PlaceLikeRepository
) {
    private val logger = LoggerFactory.getLogger(SearchPlacesService::class.java)
    
    private val totalFetchSize = 10

    /**
     * 맛집 검색 및 전체 결과 반환
     * @param request 검색 요청 (검색어, 결과 개수, meetingId)
     * @return 검색 결과 목록 (좋아요 순으로 정렬됨)
     */
    suspend fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse = coroutineScope {
        val queryKey = request.query.trim().lowercase()
        
        // 1. Google Places API 호출 (10개)
        val response = fetchPlacesFromGoogle(queryKey)
        val allPlaces = response.places ?: return@coroutineScope PlacesSearchResponse(emptyList())
        
        // 2. 전체 10개에 대해 Details 조회 및 DB 저장 (배치)
        val allPlaceDetails = placeDetailsProcessor.fetchPlaceDetailsInParallel(allPlaces)
        
        // 3. meetingId가 있으면 MeetingPlace 생성 또는 조회
        val meetingPlaces = if (request.meetingId != null) {
            val placeDbIds = getPlaceDbIds(allPlaceDetails.map { it.placeId })
            createOrGetMeetingPlaces(request.meetingId, placeDbIds)
        } else {
            emptyList()
        }
        
        // 4. Google Place ID -> DB Place ID 매핑
        val googlePlaceIds = allPlaceDetails.map { it.placeId }
        val placeIdMap = getPlaceStringIdToDbIdMap(googlePlaceIds)
        
        // 5. 좋아요 정보 매핑 (PlaceLike 테이블 기반)
        val likesMap = if (meetingPlaces.isNotEmpty()) {
            buildLikesMap(googlePlaceIds, meetingPlaces, request.userId)
        } else {
            emptyMap()
        }
        
        // 6. infra 레이어의 결과를 API 응답 DTO로 변환
        val items = allPlaceDetails.mapNotNull { detail ->
            // DB Place ID가 없으면 스킵 (이론적으로는 발생하지 않아야 함)
            val placeDbId = placeIdMap[detail.placeId] ?: return@mapNotNull null
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
                    PlacesSearchResponse.PlaceItem.AddressDescriptor(
                        description = desc
                    )
                },
                likeCount = likeInfo.likeCount,
                isLiked = likeInfo.isLiked
            )
        }
        
        // 7. meetingId가 있으면 좋아요 많은 순으로 정렬, 없으면 구글 기본 순서 유지
        val sortedItems = if (request.meetingId != null) {
            items.sortedByDescending { it.likeCount }
        } else {
            items
        }
        
        PlacesSearchResponse(sortedItems)
    }

    /**
     * Google Place ID(String)를 DB Place ID(Long)로 변환
     */
    private suspend fun getPlaceDbIds(googlePlaceIds: List<String>): List<Long> {
        return withContext(Dispatchers.IO) {
            placeQuery.findByGooglePlaceIds(googlePlaceIds).mapNotNull { it.id }
        }
    }

    /**
     * Google Place ID(String) -> DB Place ID(Long) 매핑
     */
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

    /**
     * MeetingPlace 생성 또는 조회
     * - Place가 DB에 이미 저장되어 있음 (캐싱됨)
     * - MeetingPlace 연결이 없으면 생성
     */
    private suspend fun createOrGetMeetingPlaces(meetingId: Long, placeDbIds: List<Long>): List<MeetingPlace> {
        val existingMeetingPlaces = meetingPlaceRepository.findByMeetingId(meetingId)
        val existingPlaceIds = existingMeetingPlaces.map { it.placeId }.toSet()
        
        // 새로운 Place들만 MeetingPlace 생성
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

    /**
     * 좋아요 정보 맵 생성
     * - PlaceLike 테이블에서 좋아요 데이터를 조회하여 각 Place별 좋아요 수와 사용자 좋아요 여부 계산
     */
    private suspend fun buildLikesMap(
        googlePlaceIds: List<String>,
        meetingPlaces: List<MeetingPlace>,
        userId: Long?
    ): Map<String, PlaceLikeInfo> {
        // Google Place ID -> DB Place ID 매핑
        val placeStringIdToDbId = getPlaceStringIdToDbIdMap(googlePlaceIds)
        
        // MeetingPlace ID 목록 추출
        val meetingPlaceIds = meetingPlaces.mapNotNull { it.id }
        
        if (meetingPlaceIds.isEmpty()) {
            return emptyMap()
        }
        
        // PlaceLike 조회 (한 번의 쿼리로 모든 좋아요 조회)
        val placeLikes = placeLikeRepository.findByMeetingPlaceIds(meetingPlaceIds)
        
        // MeetingPlace ID -> Place DB ID 매핑
        val meetingPlaceIdToPlaceDbId = meetingPlaces
            .filter { it.id != null }
            .associate { it.id!! to it.placeId }
        
        // Place DB ID별 좋아요 정보 그룹화
        val likesByPlaceDbId = placeLikes
            .groupBy { meetingPlaceIdToPlaceDbId[it.meetingPlaceId] }
        
        // Google Place ID별 좋아요 정보 생성
        return googlePlaceIds.associateWith { googlePlaceId ->
            val placeDbId = placeStringIdToDbId[googlePlaceId]
            val likes = if (placeDbId != null) likesByPlaceDbId[placeDbId] ?: emptyList() else emptyList()
            
            PlaceLikeInfo(
                likeCount = likes.size,
                isLiked = userId != null && likes.any { it.userId == userId }
            )
        }
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
            }
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
    
    private data class PlaceLikeInfo(
        val likeCount: Int,
        val isLiked: Boolean
    )
}