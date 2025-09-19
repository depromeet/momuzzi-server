package org.depromeet.team3.survey

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class Survey(
    val id: Long? = null,
    val meetingId: Long,
    val participantId: Long,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
