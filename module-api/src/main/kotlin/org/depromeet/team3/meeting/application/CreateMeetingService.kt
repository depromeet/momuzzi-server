package org.depromeet.team3.meeting.application

import org.depromeet.team3.auth.UserRepository
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.request.CreateMeetingRequest
import org.depromeet.team3.meeting.dto.response.CreateMeetingResponse
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.MuzziColor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateMeetingService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository,
    private val inviteTokenService: InviteTokenService,
    private val userRepository: UserRepository
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
        val meetingId = savedMeeting.id ?: throw IllegalStateException("Meeting ID is null")

        // 사용자 정보 조회하여 닉네임 가져오기
        val user = userRepository.findById(userId)
            .orElseThrow { 
                MeetingException(
                    errorCode = ErrorCode.USER_NOT_FOUND,
                    detail = mapOf("userId" to userId)
                )
            }

        val meetingAttendee = MeetingAttendee(
            id = null,
            meetingId = meetingId,
            userId = userId,
            attendeeNickname = user.nickname,
            muzziColor = MuzziColor.DEFAULT,
            createdAt = null,
            updatedAt = null
        )
        meetingAttendeeRepository.save(meetingAttendee)

        val inviteToken = inviteTokenService.generateInviteToken(meetingId)

        return CreateMeetingResponse(meetingId, inviteToken)
    }
}