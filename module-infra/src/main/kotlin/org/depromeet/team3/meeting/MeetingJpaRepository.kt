package org.depromeet.team3.meeting

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetingJpaRepository : JpaRepository<MeetingEntity, Long> {
    fun findByHostUserId(hostUserId: Long): List<MeetingEntity>
}
