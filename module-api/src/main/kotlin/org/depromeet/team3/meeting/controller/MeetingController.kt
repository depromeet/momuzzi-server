package org.depromeet.team3.meeting.controller

import jakarta.validation.Valid
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.meeting.application.CreateMeetingService
import org.depromeet.team3.meeting.application.GetMeetingService
import org.depromeet.team3.meeting.application.InviteTokenService
import org.depromeet.team3.meeting.application.JoinMeetingService
import org.depromeet.team3.meeting.dto.request.CreateMeetingRequest
import org.depromeet.team3.meeting.dto.request.GenerateInviteTokenRequest
import org.depromeet.team3.meeting.dto.response.InviteTokenResponse
import org.depromeet.team3.meeting.dto.response.MeetingResponse
import org.depromeet.team3.meeting.dto.response.ValidateInviteTokenResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/meetings")
class MeetingController(
    private val creteMeetingService: CreateMeetingService,
    private val getMeetingService: GetMeetingService,
    private val inviteTokenService: InviteTokenService,
    private val joinMeetingService: JoinMeetingService
) {

    @GetMapping
    fun getMeeting(
        userId: Long
    ) : DpmApiResponse<List<MeetingResponse>> {
        val response = getMeetingService(userId)

        return DpmApiResponse.ok(response)
    }

    @GetMapping("/validate-invite")
    fun validateInviteToken(
        @RequestParam token: String
    ): DpmApiResponse<ValidateInviteTokenResponse> {
        val response = inviteTokenService.validateInviteToken(token)

        return DpmApiResponse.ok(response)
    }

    @PostMapping
    fun create(
        userId: Long,
        @RequestBody @Valid request: CreateMeetingRequest,
    ) : DpmApiResponse<Unit> {
        creteMeetingService(request, userId)

        return DpmApiResponse.ok()
    }

    @PostMapping("/{meetingId}/invite")
    fun createInviteLink(
        @PathVariable meetingId: Long,
        @RequestBody @Valid request: GenerateInviteTokenRequest
    ): DpmApiResponse<InviteTokenResponse> {
        val response = inviteTokenService.generateInviteToken(
            meetingId = meetingId,
            baseUrl = request.baseUrl
        )

        return DpmApiResponse.ok(response)
    }

    @PostMapping("/{meetingId}/join")
    fun join(
        @PathVariable("meetingId") meetingId: Long,
        userId: Long,
    ) : DpmApiResponse<Unit> {
        joinMeetingService.invoke(meetingId, userId)

        return DpmApiResponse.ok()
    }
}