package org.depromeet.team3.restaurant

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class Restaurant(
    val id: Long? = null,
    val name: String,
    val category: String,
    val rating: Double,
    val reviewCount: Int,
    val address: String,
    val closestStation: String,
    val workingHours: String,
    val phoneNo: String,
    val descriptions: String,
    val isDeleted: Boolean = false,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
