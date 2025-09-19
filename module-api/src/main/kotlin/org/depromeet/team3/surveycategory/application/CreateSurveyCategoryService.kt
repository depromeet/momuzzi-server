package org.depromeet.team3.surveycategory.application

import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.dto.request.CreateSurveyCategoryRequest
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