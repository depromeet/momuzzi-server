package org.depromeet.team3.place

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlaceJpaRepository : JpaRepository<PlaceEntity, Long> {
}
