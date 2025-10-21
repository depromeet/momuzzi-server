package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.request.CreateMeetingRequest
import org.depromeet.team3.meeting.dto.response.CreateMeetingResponse
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.MuzziColor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateMeetingService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(request: CreateMeetingRequest, userId: Long): CreateMeetingResponse {
        val meeting = Meeting(
            id = null,
            name = request.name,
            hostUserId = userId,
            attendeeCount = request.attendeeCount,
            isClosed = false,
            stationId = request.stationId,
            endAt = request.endAt,
            createdAt = null,
            updatedAt = null
        )

        val savedMeeting = meetingRepository.save(meeting)

        val meetingAttendee = MeetingAttendee(
            id = null,
            meetingId = savedMeeting.id!!,
            userId = userId,
            attendeeNickname = null,
            muzziColor = MuzziColor.DEFAULT,
            createdAt = null,
            updatedAt = null
        )

        meetingAttendeeRepository.save(meetingAttendee)

        return CreateMeetingResponse(savedMeeting.id!!)
    }
}