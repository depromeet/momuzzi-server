package org.depromeet.team3.survey.dto.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class SurveyCreateRequest(
    @field:NotNull(message = "참가자 ID는 필수입니다")
    val participantId: Long,
    
    @field:NotEmpty(message = "닉네임은 필수입니다")
    val nickname: String,
    
    @field:NotEmpty(message = "선호 음식 목록은 필수입니다")
    val preferredCuisineList: List<String>,
    
    val avoidIngredientList: List<String> = emptyList(),
    
    val avoidMenuList: List<String> = emptyList()
)
