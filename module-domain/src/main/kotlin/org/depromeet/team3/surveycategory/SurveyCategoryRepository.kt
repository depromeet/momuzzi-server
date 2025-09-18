package org.depromeet.team3.survey_category

interface SurveyCategoryRepository {

    fun save(surveyCategory: SurveyCategory): SurveyCategory

    fun findActive(): List<SurveyCategory>
}