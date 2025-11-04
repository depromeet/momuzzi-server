package org.depromeet.team3.surveyresult

interface SurveyResultRepository {
    fun save(surveyResult: SurveyResult): SurveyResult
    fun saveAll(surveyResults: List<SurveyResult>): List<SurveyResult>
    fun findBySurveyId(surveyId: Long): List<SurveyResult>
    fun findBySurveyIdIn(surveyIds: List<Long>): List<SurveyResult>
}
