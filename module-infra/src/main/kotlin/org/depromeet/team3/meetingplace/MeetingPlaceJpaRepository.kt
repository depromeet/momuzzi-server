package org.depromeet.team3.meetingplace

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetingPlaceJpaRepository : JpaRepository<MeetingPlaceEntity, Long> {
    fun findByMeetingId(meetingId: Long): List<MeetingPlaceEntity>
    fun findByMeetingIdAndPlaceId(meetingId: Long, placeId: Long): MeetingPlaceEntity?
    fun deleteByMeetingId(meetingId: Long)
    fun existsByMeetingIdAndPlaceId(meetingId: Long, placeId: Long): Boolean
}
