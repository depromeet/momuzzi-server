package org.depromeet.team3.station

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StationJpaRepository : JpaRepository<StationEntity, Long> {
}
