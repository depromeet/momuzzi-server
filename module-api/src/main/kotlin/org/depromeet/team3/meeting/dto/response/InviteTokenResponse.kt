package org.depromeet.team3.meeting.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class InviteTokenResponse(
    @Schema(description = "토큰 검증 링크", example = "https://app.momuzzi.site/meetings/validate-invite?token=MSK@KS")
    val validateTokenUrl: String
)

