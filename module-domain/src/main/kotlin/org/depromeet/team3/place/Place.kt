package org.depromeet.team3.place

import org.depromeet.team3.common.BaseTimeDomain
import org.depromeet.team3.menu.Menu
import java.time.LocalDateTime

data class Place(
    val id: Long? = null,
    val googlePlaceId: String? = null,
    val name: String,
    val address: String,
    val rating: Double,
    val userRatingsTotal: Int,
    val openNow: Boolean? = null,
    val link: String? = null,
    val weekdayText: String? = null,
    val topReviewRating: Double? = null,
    val topReviewText: String? = null,
    val priceRangeStart: String? = null,
    val priceRangeEnd: String? = null,
    val addressDescriptor: String? = null,
    val isDeleted: Boolean = false,
    val menus: MutableList<Menu> = mutableListOf(),
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt)
