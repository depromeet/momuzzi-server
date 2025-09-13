package org.depromeet.team3.meeting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class GenerateInviteTokenRequest(
    @Schema(description = "앱의 기본 URL", example = "https://app.momuzzi.com")
    val baseUrl: String,
)
