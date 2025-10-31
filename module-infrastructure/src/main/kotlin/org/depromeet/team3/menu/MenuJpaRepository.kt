package org.depromeet.team3.menu

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MenuJpaRepository : JpaRepository<MenuEntity, Long> {
    fun findAllByPlaceId(placeId: Long): List<MenuEntity>
}
