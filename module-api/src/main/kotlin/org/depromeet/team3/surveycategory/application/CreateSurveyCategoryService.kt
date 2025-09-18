package org.depromeet.team3.survey_category.application

import org.depromeet.team3.survey_category.SurveyCategory
import org.depromeet.team3.survey_category.SurveyCategoryRepository
import org.depromeet.team3.survey_category.dto.request.CreateSurveyCategoryRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateSurveyCategoryService(
    private val surveyCategoryRepository: SurveyCategoryRepository
) {

    @Transactional
    operator fun invoke(request: CreateSurveyCategoryRequest): Unit {
        val surveyCategory = SurveyCategory(
            parentId = request.parentId,
            type = request.type,
            level = request.level,
            name = request.name,
            order = request.order,
            isDeleted = false
        )

        surveyCategoryRepository.save(surveyCategory)
    }
}