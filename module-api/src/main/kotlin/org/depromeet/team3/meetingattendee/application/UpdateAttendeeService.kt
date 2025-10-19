package org.depromeet.team3.meetingattendee.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.MuzziColor
import org.depromeet.team3.meetingattendee.exception.MeetingAttendeeException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateAttendeeService(
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(
        userId: Long,
        meetingId: Long,
        attendeeNickname: String,
        color: String?
    ): Unit {
        val attendee = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId)
            ?: throw MeetingAttendeeException(
                errorCode = ErrorCode.PARTICIPANT_NOT_FOUND,
                detail = mapOf(
                    "meetingId" to meetingId,
                    "userId" to userId
                )
            )
        attendee.attendeeNickname = attendeeNickname
        attendee.muzziColor = MuzziColor.getOrDefault(color)

        meetingAttendeeRepository.save(attendee)
    }
}