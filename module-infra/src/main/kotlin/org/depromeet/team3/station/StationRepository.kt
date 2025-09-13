package org.depromeet.team3.station

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StationRepository : JpaRepository<StationEntity, Long> {
}
