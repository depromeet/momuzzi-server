package org.depromeet.team3.surveyresult

import org.springframework.data.jpa.repository.JpaRepository

interface SurveyResultJpaRepository : JpaRepository<SurveyResultEntity, Long> {
    fun findBySurveyId(surveyId: Long): List<SurveyResultEntity>
    fun findBySurveyIdIn(surveyIds: List<Long>): List<SurveyResultEntity>
}
