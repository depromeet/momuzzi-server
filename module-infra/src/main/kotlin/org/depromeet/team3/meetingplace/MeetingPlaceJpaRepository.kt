package org.depromeet.team3.meetingplace

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository

@Repository
interface MeetingPlaceJpaRepository : JpaRepository<MeetingPlaceEntity, Long> {
    fun findByMeetingId(meetingId: Long): List<MeetingPlaceEntity>
    fun findByMeetingIdAndPlaceId(meetingId: Long, placeId: Long): MeetingPlaceEntity?
    fun existsByMeetingIdAndPlaceId(meetingId: Long, placeId: Long): Boolean

    @Modifying
    fun deleteByMeetingId(meetingId: Long)
}
