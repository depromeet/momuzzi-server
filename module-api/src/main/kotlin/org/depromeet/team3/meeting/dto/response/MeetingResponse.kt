package org.depromeet.team3.meeting.dto.response

import java.time.LocalDateTime

data class MeetingResponse(
    val id: Long,
    val hostUserId: Long,
    val attendeeCount: Int,
    val isClosed: Boolean,
    val stationId: Long,
    val endAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)