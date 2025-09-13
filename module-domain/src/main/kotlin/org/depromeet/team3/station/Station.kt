package org.depromeet.team3.station

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class Station(
    val id: Long? = null,
    val meetingId: Long,
    val name: String,
    val locX: Double,
    val locY: Double,
    val isDeleted: Boolean = false,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
