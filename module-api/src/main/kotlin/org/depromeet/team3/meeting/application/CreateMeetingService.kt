package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.request.CreateMeetingRequest
import org.depromeet.team3.meeting.dto.response.CreateMeetingResponse
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateMeetingService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(request: CreateMeetingRequest, userId: Long)
    : CreateMeetingResponse {
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

        val save = meetingRepository.save(meeting)

        val meetingAttendee = MeetingAttendee(
            null,
            save.id!!,
            userId,
            request.attendeeNickname,
            null, null
        )

        meetingAttendeeRepository.save(meetingAttendee)

        return CreateMeetingResponse(save.id!!)
    }
}