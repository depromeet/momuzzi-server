package org.depromeet.team3.meetingplacesearch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MeetingPlaceSearchRepository : JpaRepository<MeetingPlaceSearchEntity, Long> {
    fun findByMeetingId(meetingId: Long): MeetingPlaceSearchEntity?
    
    @Modifying
    @Query("DELETE FROM MeetingPlaceSearchEntity m WHERE m.expiresAt < :now")
    fun deleteExpired(@Param("now") now: LocalDateTime): Int
}
