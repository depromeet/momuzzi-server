package org.depromeet.team3.meetingattendee

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MeetingAttendeeJpaRepository : JpaRepository<MeetingAttendeeEntity, Long> {
    fun findByMeetingId(meetingId: Long): List<MeetingAttendeeEntity>
    fun findByUserId(userId: Long): List<MeetingAttendeeEntity>
    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingAttendeeEntity?
    fun countByMeetingId(meetingId: Long): Int
    fun existsByMeetingIdAndUserId(meetingId: Long, userId: Long): Boolean

    @Query("SELECT COUNT(ma) > 0 FROM MeetingAttendeeEntity ma WHERE ma.meeting.id = :meetingId AND ma.attendeeNickname = :nickname")
    fun existsByMeetingIdAndNickname(
        @Param("meetingId") meetingId: Long,
        @Param("nickname") nickname: String
    ): Boolean
}
