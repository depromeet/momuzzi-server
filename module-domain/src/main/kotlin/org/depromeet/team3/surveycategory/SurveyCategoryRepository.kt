package org.depromeet.team3.survey_category

interface SurveyCategoryRepository {

    fun save(surveyCategory: SurveyCategory): SurveyCategory

    fun findById(id: Long): SurveyCategory?

    fun findActive(): List<SurveyCategory>

    fun existsByParentIdAndIsDeletedFalse(parentId: Long): Boolean
}