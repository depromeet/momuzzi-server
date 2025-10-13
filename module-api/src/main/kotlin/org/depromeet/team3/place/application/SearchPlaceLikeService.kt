package org.depromeet.team3.place.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.meetingplace.exception.MeetingPlaceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchPlaceLikeService(
    private val meetingPlaceRepository: MeetingPlaceRepository
) {

    @Transactional
    suspend fun toggle(meetingId: Long, userId: Long, placeId: Long): Boolean {

        val meetingPlace = meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId)
            ?: throw MeetingPlaceException(
                errorCode = ErrorCode.MEETING_PLACE_NOT_FOUND,
                detail = mapOf(
                    "meetingId" to meetingId,
                    "placeId" to placeId
                )
            )

        val updated = meetingPlace.toggleLike(userId)
        meetingPlaceRepository.save(updated)

        return updated.isLikedBy(userId)
    }
}
