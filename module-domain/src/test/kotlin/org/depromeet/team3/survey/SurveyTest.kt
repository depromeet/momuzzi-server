package org.depromeet.team3.survey

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("[SURVEY] 설문 도메인 테스트")
class SurveyTest {

    @Test
    @DisplayName("Survey 도메인 객체를 생성한다")
    fun `Survey 도메인 객체를 생성한다`() {
        // given
        val meetingId = 1L
        val participantId = 2L

        // when
        val survey = Survey(
            meetingId = meetingId,
            participantId = participantId
        )

        // then
        assert(survey.meetingId == meetingId)
        assert(survey.participantId == participantId)
        assert(survey.id == null)
    }
}