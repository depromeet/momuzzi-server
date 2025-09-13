package org.depromeet.team3.menu

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class Menu(
    val id: Long? = null,
    val restaurantId: Long,
    val name: String,
    val category: String,
    val price: Int,
    val isDeleted: Boolean = false,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
