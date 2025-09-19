package org.depromeet.team3.surveyresult

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("[SURVEY RESULT] 설문 결과 도메인 테스트")
class SurveyResultTest {

    @Test
    @DisplayName("SurveyResult 도메인 객체를 생성한다")
    fun `SurveyResult 도메인 객체를 생성한다`() {
        // given
        val surveyId = 1L
        val surveyCategoryId = 2L

        // when
        val surveyResult = SurveyResult(
            surveyId = surveyId,
            surveyCategoryId = surveyCategoryId
        )

        // then
        assert(surveyResult.surveyId == surveyId)
        assert(surveyResult.surveyCategoryId == surveyCategoryId)
        assert(surveyResult.id == null)
    }
}