package org.depromeet.team3.survey_category.application

import org.depromeet.team3.survey_category.SurveyCategory
import org.depromeet.team3.survey_category.SurveyCategoryRepository
import org.depromeet.team3.survey_category.dto.request.UpdateSurveyCategoryRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateSurveyCategoryService(
    private val surveyCategoryRepository: SurveyCategoryRepository
) {

    @Transactional
    operator fun invoke(id: Long, request: UpdateSurveyCategoryRequest): Unit {
        // 1. 기존 카테고리 조회
        val existingCategory = surveyCategoryRepository.findById(id)
            ?: throw IllegalArgumentException("카테고리를 찾을 수 없습니다. ID: $id")

        // 2. 업데이트된 카테고리 생성
        val updatedCategory = existingCategory.copy(
            parentId = request.parentId,
            type = request.type,
            level = request.level,
            name = request.name,
            order = request.order
        )

        // 3. 저장
        surveyCategoryRepository.save(updatedCategory)
    }
}
