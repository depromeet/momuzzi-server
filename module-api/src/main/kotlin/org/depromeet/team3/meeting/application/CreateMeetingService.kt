package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.request.CreateMeetingRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateMeetingService(
    private val meetingRepository: MeetingRepository
) {

    @Transactional
    operator fun invoke(request: CreateMeetingRequest, userId: Long): Unit {
        val meeting = Meeting(
            null,
            request.name,
            userId,
            request.attendeeCount,
            isClosed = false,
            request.stationId,
            request.endAt,
            null, null
        )

        meetingRepository.save(meeting)
    }
}