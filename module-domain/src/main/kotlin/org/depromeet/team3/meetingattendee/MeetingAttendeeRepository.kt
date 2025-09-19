package org.depromeet.team3.meetingattendee

interface MeetingAttendeeRepository {

    fun save(meetingAttendee: MeetingAttendee): MeetingAttendee

    fun findByMeetingId(meetingId: Long): List<MeetingAttendee>

    fun findByUserId(userId: Long): List<MeetingAttendee>

    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingAttendee?

    fun existsByMeetingIdAndUserId(meetingId: Long, userId: Long): Boolean

    fun deleteById(id: Long)
}