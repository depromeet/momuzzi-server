package org.depromeet.team3.meeting.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.exception.MeetingAttendeeException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JoinMeetingService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(
        userId: Long,
        meetingId: Long,
        attendeeNickname: String
    ): Unit {
        validateMeeting(meetingId, userId)

        val meetingAttendee = MeetingAttendee(
            null,
            meetingId,
            userId,
            attendeeNickname,
            null, null
        )

        meetingAttendeeRepository.save(meetingAttendee)
    }

    private fun validateMeeting(meetingId: Long, userId: Long) {
        val meeting = meetingRepository.findById(meetingId)
            ?: throw MeetingException(
                errorCode = ErrorCode.MEETING_NOT_FOUND,
                detail = mapOf("meetingId" to meetingId)
            )

        if (meeting.isClosed) {
            throw MeetingException(
                errorCode = ErrorCode.MEETING_ALREADY_CLOSED,
                detail = mapOf("meetingId" to meetingId)
            )
        }

        meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId)?.let {
            throw MeetingAttendeeException(
                errorCode = ErrorCode.MEETING_ALREADY_JOINED,
                detail = mapOf(
                    "meetingId" to meetingId,
                    "userId" to userId
                )
            )
        }

        val currentAttendeeCount = meetingAttendeeRepository.countByMeetingId(meetingId)
        if (currentAttendeeCount >= meeting.attendeeCount) {
            throw MeetingException(
                errorCode = ErrorCode.MEETING_FULL,
                detail = mapOf(
                    "meetingId" to meetingId,
                    "currentCount" to currentAttendeeCount,
                    "maxCount" to meeting.attendeeCount
                )
            )
        }
    }
}
