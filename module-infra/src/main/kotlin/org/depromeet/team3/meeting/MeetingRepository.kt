package org.depromeet.team3.meeting

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MeetingRepository : JpaRepository<MeetingEntity, Long> {
}
