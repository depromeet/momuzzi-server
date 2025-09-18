package org.depromeet.team3.survey_category.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.depromeet.team3.survey_category.SurveyCategoryLevel

@Schema(description = "설문 카테고리 계층 구조 아이템")
data class SurveyCategoryItem(
    @Schema(description = "카테고리 레벨", example = "BRANCH")
    val level: SurveyCategoryLevel,
    
    @Schema(description = "카테고리명", example = "한식")
    val name: String,
    
    @Schema(description = "카테고리 순서", example = "1")
    val order: Int,
    
    @Schema(description = "하위 카테고리 목록")
    val children: List<SurveyCategoryItem> = emptyList()
)
