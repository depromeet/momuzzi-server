package org.depromeet.team3.survey.dto.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class SurveyCreateRequest(
    @field:NotNull(message = "참가자 ID는 필수입니다")
    val participantId: Long,
    
    @field:NotEmpty(message = "닉네임은 필수입니다")
    val nickname: String,
    
    @field:NotEmpty(message = "선택한 음식 카테고리 목록은 필수입니다")
    val selectedCategoryList: List<Long>
)
