package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.response.MeetingResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMeetingService(
    private val meetingRepository: MeetingRepository
) {

    @Transactional(readOnly = true)
    operator fun invoke(userId: Long): List<MeetingResponse> {
        return meetingRepository.findMeetingsByUserId(userId)
            .map { meeting ->
                MeetingResponse(
                    id = meeting.id!!,
                    hostUserId = meeting.hostUserId,
                    attendeeCount = meeting.attendeeCount,
                    isClosed = meeting.isClosed,
                    stationId = meeting.stationId,
                    endAt = meeting.endAt!!,
                    createdAt = meeting.createdAt!!,
                    updatedAt = meeting.updatedAt
                )
            }
    }
}