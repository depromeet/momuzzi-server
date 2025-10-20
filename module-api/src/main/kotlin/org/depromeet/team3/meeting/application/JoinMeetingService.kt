package org.depromeet.team3.meeting.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.exception.InvalidInviteTokenException
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.MuzziColor
import org.depromeet.team3.meetingattendee.exception.MeetingAttendeeException
import org.depromeet.team3.util.DataEncoder
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
        token: String,
    ): Unit {
        val meetingId = parseTokenId(token)
        validateMeeting(meetingId, userId)

        val meetingAttendee = MeetingAttendee(
            null,
            meetingId,
            userId,
            null,
            MuzziColor.DEFAULT,
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

    private fun parseTokenId(token: String): Long {
        val parts = DataEncoder.decodeWithSeparator(token, ":")
            ?.takeIf { it.size == 2 }
            ?: throw InvalidInviteTokenException(ErrorCode.INVALID_TOKEN_FORMAT)

        return parts[0].toLongOrNull()
            ?: throw InvalidInviteTokenException(ErrorCode.INVALID_MEETING_ID_IN_TOKEN)
    }
}
