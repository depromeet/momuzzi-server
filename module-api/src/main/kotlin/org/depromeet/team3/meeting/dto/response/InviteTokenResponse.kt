package org.depromeet.team3.meeting.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class InviteTokenResponse(
    @Schema(description = "초대 링크 URL", example = "https://app.momuzzi.com/meetings/join?token=abc123")
    val inviteUrl: String,
    
    @Schema(description = "초대 토큰", example = "abc123")
    val token: String,
)
