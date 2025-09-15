package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.response.InviteTokenResponse
import org.depromeet.team3.meeting.dto.response.ValidateInviteTokenResponse
import org.depromeet.team3.util.DataEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

@Service
class InviteTokenService(
    private val meetingRepository: MeetingRepository
) {
    
    private companion object {
        const val SEPARATOR = ":"
    }

    @Transactional(readOnly = true)
    fun generateInviteToken(
        meetingId: Long, 
        baseUrl: String,
    ): InviteTokenResponse {
        val meeting = meetingRepository.findById(meetingId)
            ?: throw IllegalArgumentException("Not Found meeting ID: $meetingId")

        if (meeting.isClosed) {
            throw IllegalStateException("Ended meeting ID: $meetingId")
        }

        val endAtTimestamp = meeting.endAt!!.toInstant(ZoneOffset.UTC).toEpochMilli()
        
        val encodedData = DataEncoder.encodeWithSeparator(SEPARATOR, meetingId.toString(), endAtTimestamp.toString())
        val inviteUrl = "$baseUrl/meetings/join?token=$encodedData"
        
        return InviteTokenResponse(
            inviteUrl = inviteUrl,
            token = encodedData
        )
    }

    @Transactional(readOnly = true)
    fun validateInviteToken(
        token: String,
    ): ValidateInviteTokenResponse {
        val decodedData = DataEncoder.decodeWithSeparator(token, SEPARATOR)
            ?: return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = true,
                meetingId = null,
                message = "유효하지 않은 토큰입니다."
            )
        
        if (decodedData.size != 2) {
            return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = false,
                meetingId = null,
                message = "토큰 형식이 올바르지 않습니다."
            )
        }
        
        val meetingId = try {
            decodedData[0].toLong()
        } catch (e: NumberFormatException) {
            return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = false,
                meetingId = null,
                message = "토큰의 모임 ID 형식이 올바르지 않습니다."
            )
        }
        
        val endAtTimestamp = try {
            decodedData[1].toLong()
        } catch (e: NumberFormatException) {
            return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = false,
                meetingId = meetingId,
                message = "토큰의 만료 시간 형식이 올바르지 않습니다."
            )
        }

        val meeting = meetingRepository.findById(meetingId)
            ?: return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = false,
                meetingId = meetingId,
                message = "존재하지 않는 모임입니다."
            )
        
        if (meeting.isClosed) {
            return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = false,
                meetingId = meetingId,
                message = "이미 종료된 모임입니다."
            )
        }

        val isExpired = System.currentTimeMillis() > endAtTimestamp
        if (isExpired) {
            return ValidateInviteTokenResponse(
                isValid = false,
                isExpired = true,
                meetingId = meetingId,
                message = "만료된 토큰입니다."
            )
        }
        
        return ValidateInviteTokenResponse(
            isValid = true,
            isExpired = false,
            meetingId = meetingId,
            message = "유효한 토큰입니다."
        )
    }
}
