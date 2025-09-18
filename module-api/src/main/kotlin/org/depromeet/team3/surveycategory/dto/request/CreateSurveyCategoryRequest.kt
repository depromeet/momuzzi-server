package org.depromeet.team3.survey_category.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import org.depromeet.team3.survey_category.SurveyCategoryType
import org.depromeet.team3.survey_category.SurveyCategoryLevel

@Schema(description = "설문 카테고리 생성 요청 DTO")
data class CreateSurveyCategoryRequest(

    @Schema(description = "상위 카테고리 ID", example = "1", nullable = true)
    val parentId: Long? = null,

    @Schema(description = "카테고리 타입", example = "CUISINE")
    val type: SurveyCategoryType,

    @Schema(description = "카테고리 레벨", example = "BRANCH")
    val level: SurveyCategoryLevel,

    @Schema(description = "카테고리명", example = "한식")
    val name: String,

    @Schema(description = "카테고리 순서", example = "1")
    val order: Int
)