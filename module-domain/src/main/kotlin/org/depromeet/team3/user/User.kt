package org.depromeet.team3.user

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val kakaoId: String,
    val email: String,
    val nickname: String,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
