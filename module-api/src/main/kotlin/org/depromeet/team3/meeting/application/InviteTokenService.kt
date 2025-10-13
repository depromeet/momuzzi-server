package org.depromeet.team3.meeting.application

import org.depromeet.team3.common.ContextConstants.API_VERSION_V1
import org.depromeet.team3.common.ContextConstants.BASE_DOMAIN
import org.depromeet.team3.common.ContextConstants.HTTPS_PROTOCOL
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.response.InviteTokenResponse
import org.depromeet.team3.meeting.dto.response.ValidateInviteTokenResponse
import org.depromeet.team3.meeting.exception.InvalidInviteTokenException
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.util.DataEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

@Service
class InviteTokenService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {
    
    private companion object {
        const val SEPARATOR = ":"
    }

    @Transactional(readOnly = true)
    fun generateInviteToken(
        meetingId: Long
    ): InviteTokenResponse {
        val meeting = meetingRepository.findById(meetingId)
            ?: throw IllegalArgumentException("Not Found meeting ID: $meetingId")

        if (meeting.isClosed) {
            throw IllegalStateException("Ended meeting ID: $meetingId")
        }

        val endAtTimestamp = meeting.endAt!!.toInstant(ZoneOffset.UTC).toEpochMilli()
        
        val encodedData = DataEncoder.encodeWithSeparator(SEPARATOR, meetingId.toString(), endAtTimestamp.toString())
        val validateTokenUrl = "$HTTPS_PROTOCOL/$BASE_DOMAIN/$API_VERSION_V1/meetings/validate-invite?token=$encodedData"
        
        return InviteTokenResponse(validateTokenUrl)
    }

    @Transactional(readOnly = true)
    fun validateInviteToken(token: String): ValidateInviteTokenResponse {
        val (meetingId, expiryTimestamp) = parseTokenData(token)

        if (System.currentTimeMillis() > expiryTimestamp) {
            throw InvalidInviteTokenException(ErrorCode.TOKEN_EXPIRED)
        }

        val meeting = meetingRepository.findById(meetingId)
            ?: throw InvalidInviteTokenException(ErrorCode.MEETING_NOT_FOUND_FOR_TOKEN)

        if (meeting.isClosed) {
            throw InvalidInviteTokenException(ErrorCode.MEETING_ALREADY_CLOSED)
        }

        val currentAttendeeCount = meetingAttendeeRepository.countByMeetingId(meetingId)
        if (currentAttendeeCount >= meeting.attendeeCount) {
            throw IllegalStateException("Meeting is full. Current: $currentAttendeeCount, Max: ${meeting.attendeeCount}")
        }

        return ValidateInviteTokenResponse(meetingId)
    }

    private fun parseTokenData(token: String): Pair<Long, Long> {
        val parts = DataEncoder.decodeWithSeparator(token, SEPARATOR)
            ?.takeIf { it.size == 2 }
            ?: throw InvalidInviteTokenException(ErrorCode.INVALID_TOKEN_FORMAT)

        val meetingId = parts[0].toLongOrNull()
            ?: throw InvalidInviteTokenException(ErrorCode.INVALID_MEETING_ID_IN_TOKEN)

        val expiryTimestamp = parts[1].toLongOrNull()
            ?: throw InvalidInviteTokenException(ErrorCode.INVALID_EXPIRY_TIME_IN_TOKEN)

        return Pair(meetingId, expiryTimestamp)
    }
}
