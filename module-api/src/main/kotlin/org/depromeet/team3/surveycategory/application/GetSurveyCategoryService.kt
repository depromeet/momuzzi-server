package org.depromeet.team3.surveycategory.application

import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryQuery
import org.depromeet.team3.surveycategory.SurveyCategoryType
import org.depromeet.team3.surveycategory.dto.response.SurveyCategoryItem
import org.depromeet.team3.surveycategory.dto.response.SurveyCategoryResponse
import org.springframework.stereotype.Service

@Service
class GetSurveyCategoryService(
    private val surveyCategoryQuery: SurveyCategoryQuery
) {

    operator fun invoke(): SurveyCategoryResponse {
        // DB에서 모든 활성 카테고리 조회
        val allCategories = surveyCategoryQuery.findActive()
        
        // 카테고리 타입별로 분류하여 계층 구조 생성
        val preferredCuisineCategories = buildHierarchyFromDatabase(
            allCategories.filter { it.type == SurveyCategoryType.CUISINE }
        )
        val avoidIngredientCategories = buildHierarchyFromDatabase(
            allCategories.filter { it.type == SurveyCategoryType.AVOID_INGREDIENT }
        )
        val avoidMenuCategories = buildHierarchyFromDatabase(
            allCategories.filter { it.type == SurveyCategoryType.AVOID_MENU }
        )

        return SurveyCategoryResponse(
            preferredCuisineCategoryList = preferredCuisineCategories,
            avoidIngredientCategoryList = avoidIngredientCategories,
            avoidMenuCategoryList = avoidMenuCategories
        )
    }

    private fun buildHierarchyFromDatabase(categories: List<SurveyCategory>): List<SurveyCategoryItem> {
        val categoryMap = categories.associateBy { it.id }
        val rootCategories = categories.filter { it.parentId == null }
            .sortedBy { it.order }
        
        return rootCategories.map { category ->
            buildCategoryItem(category, categoryMap)
        }
    }

    private fun buildCategoryItem(category: SurveyCategory, categoryMap: Map<Long?, SurveyCategory>): SurveyCategoryItem {
        val children = categoryMap.values
            .filter { it.parentId == category.id }
            .sortedBy { it.order }
            .map { buildCategoryItem(it, categoryMap) }
        
        return SurveyCategoryItem(
            level = category.level,
            name = category.name,
            order = category.order,
            children = children
        )
    }
}