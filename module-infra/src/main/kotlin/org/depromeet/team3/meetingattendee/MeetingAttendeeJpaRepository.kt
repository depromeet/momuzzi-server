package org.depromeet.team3.meetingattendee

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetingAttendeeJpaRepository : JpaRepository<MeetingAttendeeEntity, Long> {
    fun findByMeetingId(meetingId: Long): List<MeetingAttendeeEntity>
    fun findByUserId(userId: Long): List<MeetingAttendeeEntity>
    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingAttendeeEntity?
    fun existsByMeetingIdAndUserId(meetingId: Long, userId: Long): Boolean
}
