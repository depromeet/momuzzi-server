package org.depromeet.team3.place.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
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
    private val placeDetailsProcessor: PlaceDetailsProcessor,
    private val meetingPlaceRepository: MeetingPlaceRepository
) {
    private val logger = LoggerFactory.getLogger(SearchPlacesService::class.java)
    
    private val totalFetchSize = 10

    /**
     * 맛집 검색 및 전체 결과 반환
     *
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
        
        // 4. 좋아요 정보 매핑 (MeetingPlace에 좋아요 정보 포함되어 있음)
        val likesMap = if (meetingPlaces.isNotEmpty()) {
            val placeStringIdToDbId = getPlaceStringIdToDbIdMap(allPlaceDetails.map { it.placeId })
            val meetingPlaceByPlaceDbId = meetingPlaces.associateBy { it.placeId }
            
            allPlaceDetails.associate { detail ->
                val placeDbId = placeStringIdToDbId[detail.placeId]
                val meetingPlace = if (placeDbId != null) meetingPlaceByPlaceDbId[placeDbId] else null
                
                detail.placeId to PlaceLikeInfo(
                    likeCount = meetingPlace?.likeCount ?: 0,
                    isLiked = if (request.userId != null && meetingPlace != null) {
                        meetingPlace.isLikedBy(request.userId)
                    } else {
                        false
                    }
                )
            }
        } else {
            emptyMap()
        }
        
        // 5. infra 레이어의 결과를 API 응답 DTO로 변환
        val items = allPlaceDetails.map { detail ->
            val likeInfo = likesMap[detail.placeId] ?: PlaceLikeInfo(0, false)
            
            PlacesSearchResponse.PlaceItem(
                placeId = detail.placeId,
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
        
        // 6. meetingId가 있으면 좋아요 많은 순으로 정렬, 없으면 구글 기본 순서 유지
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
                    place.googlePlaceId?.let { it to place.id!! }
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
    
    private data class PlaceLikeInfo(
        val likeCount: Int,
        val isLiked: Boolean
    )
}
