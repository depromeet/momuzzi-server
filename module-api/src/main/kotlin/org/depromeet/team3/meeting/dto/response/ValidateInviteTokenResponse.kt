package org.depromeet.team3.meeting.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ValidateInviteTokenResponse(
    @Schema(description = "토큰 유효성", example = "true")
    val isValid: Boolean,
    
    @Schema(description = "토큰 만료 여부", example = "false")
    val isExpired: Boolean,
    
    @Schema(description = "미팅 ID", example = "123")
    val meetingId: Long?,
    
    @Schema(description = "검증 결과 메시지", example = "유효한 토큰입니다.")
    val message: String
)
