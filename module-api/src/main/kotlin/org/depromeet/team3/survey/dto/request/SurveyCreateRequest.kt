package org.depromeet.team3.survey.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(description = "설문 생성 요청")
data class SurveyCreateRequest(
    @field:NotNull(message = "참가자 ID는 필수입니다")
    @Schema(description = "참가자 ID", example = "1")
    val participantId: Long,
    
    @field:NotEmpty(message = "닉네임은 필수입니다")
    @Schema(description = "참가자 닉네임", example = "홍길동")
    val nickname: String,
    
    @field:NotEmpty(message = "선택한 음식 카테고리 목록은 필수입니다")
    @Schema(description = "선택된 카테고리 ID 목록", example = "[1, 3, 5]")
    val selectedCategoryList: List<Long>
)
