package org.depromeet.team3.meeting.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.depromeet.team3.common.ContextConstants
import org.depromeet.team3.common.annotation.UserId
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.meeting.application.CreateMeetingService
import org.depromeet.team3.meeting.application.GetMeetingService
import org.depromeet.team3.meeting.application.InviteTokenService
import org.depromeet.team3.meeting.application.JoinMeetingService
import org.depromeet.team3.meeting.dto.request.CreateMeetingRequest
import org.depromeet.team3.meeting.dto.request.GenerateInviteTokenRequest
import org.depromeet.team3.meeting.dto.request.JoinMeetingRequest
import org.depromeet.team3.meeting.dto.response.CreateMeetingResponse
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

@Tag(name = "모임", description = "모임 관련 API")
@RestController
@RequestMapping("${ContextConstants.API_VERSION_V1}/meetings")
class MeetingController(
    private val creteMeetingService: CreateMeetingService,
    private val getMeetingService: GetMeetingService,
    private val inviteTokenService: InviteTokenService,
    private val joinMeetingService: JoinMeetingService
) {

    @Operation(
        summary = "사용자 모임 목록 조회",
        description = "특정 사용자가 참여한 모임 목록을 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "모임 목록 조회 성공")
    )
    @GetMapping
    fun getMeeting(
        @Parameter(description = "사용자 ID", example = "123")
        @UserId userId: Long
    ) : DpmApiResponse<List<MeetingResponse>> {
        val response = getMeetingService(userId)

        return DpmApiResponse.ok(response)
    }

    @Operation(
        summary = "모임 초대 토큰 검증",
        description = "모임 초대 토큰의 유효성을 검증합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "토큰 검증 성공")
    )
    @GetMapping("/validate-invite")
    fun validateInviteToken(
        @Parameter(description = "모임 초대 토큰", example = "abc123def456")
        @RequestParam token: String
    ): DpmApiResponse<ValidateInviteTokenResponse> {
        val response = inviteTokenService.validateInviteToken(token)

        return DpmApiResponse.ok(response)
    }

    @Operation(
        summary = "모임 생성",
        description = "새로운 모임을 생성합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "모임 생성 성공")
    )
    @PostMapping
    fun create(
        @Parameter(description = "사용자 ID", example = "123")
        @UserId userId: Long,
        @RequestBody @Valid request: CreateMeetingRequest,
    ) : DpmApiResponse<CreateMeetingResponse> {
        val response = creteMeetingService(request, userId)

        return DpmApiResponse.ok(response)
    }

    @Operation(
        summary = "모임 초대 토큰 생성",
        description = "특정 모임의 초대 토큰을 생성합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "초대 토큰 생성 성공")
    )
    @PostMapping("/{meetingId}/invite")
    fun createInviteLink(
        @Parameter(description = "모임 ID", example = "1")
        @PathVariable meetingId: Long,
        @RequestBody @Valid request: GenerateInviteTokenRequest
    ): DpmApiResponse<InviteTokenResponse> {
        val response = inviteTokenService.generateInviteToken(
            meetingId = meetingId,
            baseUrl = request.baseUrl
        )

        return DpmApiResponse.ok(response)
    }

    @Operation(
        summary = "모임 참여",
        description = "특정 모임에 참여합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "모임 참여 성공")
    )
    @PostMapping("/{meetingId}/join")
    fun join(
        @Parameter(description = "모임 ID", example = "1")
        @PathVariable("meetingId") meetingId: Long,
        @Parameter(description = "사용자 ID", example = "123")
        @UserId userId: Long,
        @RequestBody @Valid request: JoinMeetingRequest
    ) : DpmApiResponse<Unit> {
        joinMeetingService.invoke(meetingId, userId, request.attendeeNickname)

        return DpmApiResponse.ok()
    }
}