package org.depromeet.team3.user

import org.depromeet.team3.common.BaseTimeDomain
import org.depromeet.team3.meeting.Meeting
import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val email: String,
    val nickname: String,
    var profileImage: String?,
    val socialId: String,
    var refreshToken: String?,
    val meetings: MutableList<Meeting> = mutableListOf(),
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
