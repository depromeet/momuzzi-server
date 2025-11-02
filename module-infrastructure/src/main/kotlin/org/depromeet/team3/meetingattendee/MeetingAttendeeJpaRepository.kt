package org.depromeet.team3.meetingattendee

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MeetingAttendeeJpaRepository : JpaRepository<MeetingAttendeeEntity, Long> {
    fun findByMeetingId(meetingId: Long): List<MeetingAttendeeEntity>
    fun findByUserId(userId: Long): List<MeetingAttendeeEntity>

    @Query("SELECT ma FROM MeetingAttendeeEntity ma WHERE ma.meeting.id = :meetingId AND ma.user.id = :userId")
    fun findByMeetingIdAndUserId(
        @Param("meetingId") meetingId: Long,
        @Param("userId") userId: Long
    ): MeetingAttendeeEntity?

    fun countByMeetingId(meetingId: Long): Int

    @Query("SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END FROM MeetingAttendeeEntity ma WHERE ma.meeting.id = :meetingId AND ma.user.id = :userId")
    fun existsByMeetingIdAndUserId(
        @Param("meetingId") meetingId: Long,
        @Param("userId") userId: Long
    ): Boolean

    @Query("SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END FROM MeetingAttendeeEntity ma WHERE ma.meeting.id = :meetingId AND ma.attendeeNickname = :nickname")
    fun existsByMeetingIdAndNickname(
        @Param("meetingId") meetingId: Long,
        @Param("nickname") nickname: String
    ): Boolean
}
