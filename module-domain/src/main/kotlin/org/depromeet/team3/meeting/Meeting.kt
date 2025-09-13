package org.depromeet.team3.meeting

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class Meeting(
    val id: Long? = null,
    val hostUserId: Long,
    val attendeeCount: Int,
    val isClosed: Boolean = false,
    val stationId: Long,
    val endAt: LocalDateTime? = null,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt) {

    fun close(meeting: Meeting): Meeting {
        return meeting.copy(
            isClosed = true,
            endAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}