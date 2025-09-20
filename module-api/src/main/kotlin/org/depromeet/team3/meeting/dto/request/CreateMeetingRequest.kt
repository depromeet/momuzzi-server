package org.depromeet.team3.meeting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CreateMeetingRequest(

    @Schema(description = "모임 이름", example = "점심 모무찌?")
    val name: String,

    @Schema(description = "모임 생성자 닉네임", example = "나야 생성자")
    val attendeeNickname: String,

    @Schema(description = "모임 참여 인원", example = "1")
    val attendeeCount: Int,

    @Schema(description = "역 ID", example = "1")
    val stationId: Long,

    @Schema(description = "모임 종료 시간? 투표 시간?", example = "")
    val endAt: LocalDateTime? = null,
)