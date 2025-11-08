package org.depromeet.team3.placelike.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.meetingplace.exception.MeetingPlaceException
import org.depromeet.team3.placelike.PlaceLike
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchPlaceLikeService(
    private val meetingPlaceRepository: MeetingPlaceRepository,
    private val placeLikeRepository: PlaceLikeRepository
) {

    @Transactional
    suspend fun toggle(meetingId: Long, userId: Long, placeId: Long): PlaceLikeResult {
        // 1. MeetingPlace가 존재하는지 확인
        val meetingPlace = meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId)
            ?: throw MeetingPlaceException(
                errorCode = ErrorCode.MEETING_PLACE_NOT_FOUND,
                detail = mapOf(
                    "meetingId" to meetingId,
                    "placeId" to placeId
                )
            )

        val meetingPlaceId = meetingPlace.id
            ?: throw MeetingPlaceException(
                errorCode = ErrorCode.MEETING_PLACE_NOT_FOUND,
                detail = mapOf("meetingId" to meetingId, "placeId" to placeId)
            )

        // 2. 좋아요 처리
        val isLiked = try {
            placeLikeRepository.save(
                PlaceLike(
                    meetingPlaceId = meetingPlaceId,
                    userId = userId
                )
            )
            true
        } catch (e: DataIntegrityViolationException) {
            placeLikeRepository.deleteByMeetingPlaceIdAndUserId(meetingPlaceId, userId)
            false
        }

        // 3. 현재 좋아요 수 조회
        val likeCount = placeLikeRepository.countByMeetingPlaceId(meetingPlaceId).toInt()

        return PlaceLikeResult(
            isLiked = isLiked,
            likeCount = likeCount
        )
    }

    data class PlaceLikeResult(
        val isLiked: Boolean,
        val likeCount: Int
    )
}
