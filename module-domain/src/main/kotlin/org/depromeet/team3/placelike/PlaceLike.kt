package org.depromeet.team3.placelike

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class PlaceLike(
    val id: Long? = null,
    val meetingPlaceId: Long,
    val userId: Long,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)