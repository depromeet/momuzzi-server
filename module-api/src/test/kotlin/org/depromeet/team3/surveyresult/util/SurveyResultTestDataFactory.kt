package org.depromeet.team3.surveyresult.util

import org.depromeet.team3.surveyresult.SurveyResult

/**
 * SurveyResult 관련 테스트 데이터 팩토리 클래스
 */
object SurveyResultTestDataFactory {

    fun createSurveyResult(
        id: Long = 1L,
        surveyId: Long = 1L,
        surveyCategoryId: Long = 1L
    ): SurveyResult {
        return SurveyResult(
            id = id,
            surveyId = surveyId,
            surveyCategoryId = surveyCategoryId
        )
    }

    fun createSurveyResultList(
        surveyId: Long = 1L,
        categoryIds: List<Long> = listOf(1L, 2L)
    ): List<SurveyResult> {
        return categoryIds.mapIndexed { index, categoryId ->
            createSurveyResult(
                id = (index + 1).toLong(),
                surveyId = surveyId,
                surveyCategoryId = categoryId
            )
        }
    }
}
