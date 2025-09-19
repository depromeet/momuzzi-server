package org.depromeet.team3.surveyresult

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class SurveyResult(
    val id: Long? = null,
    val surveyId: Long,
    val surveyCategoryId: Long,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
