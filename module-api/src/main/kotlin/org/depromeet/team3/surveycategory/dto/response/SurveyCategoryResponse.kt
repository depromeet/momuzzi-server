package org.depromeet.team3.surveycategory.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "설문 카테고리 응답 DTO")
data class SurveyCategoryResponse(
    @Schema(description = "선호 음식 카테고리 목록")
    val preferredCuisineCategoryList: List<SurveyCategoryItem>,
    
    @Schema(description = "피해야 할 재료 카테고리 목록")
    val avoidIngredientCategoryList: List<SurveyCategoryItem>,
    
    @Schema(description = "피해야 할 메뉴 카테고리 목록")
    val avoidMenuCategoryList: List<SurveyCategoryItem>
)