package org.depromeet.team3.meetingattendee

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class MeetingAttendee(
    val id: Long? = null,
    val meetingId: Long,
    val userId: Long,
    val attendeeNickname: String?,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)