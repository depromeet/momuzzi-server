package org.depromeet.team3.surveycategory.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.depromeet.team3.surveycategory.SurveyCategoryType
import org.depromeet.team3.surveycategory.SurveyCategoryLevel

@Schema(description = "설문 카테고리 생성 요청 DTO")
data class CreateSurveyCategoryRequest(

    @Schema(description = "상위 카테고리 ID", example = "1", nullable = true)
    val parentId: Long? = null,

    @Schema(description = "카테고리 타입", example = "CUISINE")
    @field:NotNull(message = "카테고리 타입은 필수입니다")
    val type: SurveyCategoryType,

    @Schema(description = "카테고리 레벨", example = "BRANCH")
    @field:NotNull(message = "카테고리 레벨은 필수입니다")
    val level: SurveyCategoryLevel,

    @Schema(description = "카테고리명", example = "한식")
    @field:NotBlank(message = "카테고리명은 필수입니다")
    val name: String,

    @Schema(description = "카테고리 순서", example = "1")
    val order: Int
)