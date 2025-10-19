package org.depromeet.team3.meetingattendee.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class UpdateAttendeeRequest(

    @Schema(description = "모임에서 사용할 사용자 닉네임", example = "김은행나무")
    @field:NotBlank(message = "참여자 닉네임은 필수입니다.")
    val attendeeNickname: String,

    @Schema(description = "모임에서 사용할 무찌 색상", example = "수박색")
    val color: String?
)
