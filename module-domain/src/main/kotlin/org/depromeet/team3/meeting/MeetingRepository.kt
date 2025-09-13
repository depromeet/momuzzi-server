package org.depromeet.team3.meeting

interface MeetingRepository {

    fun save(meeting: Meeting): Meeting

    fun findMeetingsByUserId(userId: Long): List<Meeting>
}