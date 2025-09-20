package org.depromeet.team3.meeting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class JoinMeetingRequest(
    @Schema(description = "모임에서 사용할 닉네임", example = "삼삼오오")
    @field:NotBlank(message = "닉네임은 필수입니다")
    val attendeeNickname: String,
)
