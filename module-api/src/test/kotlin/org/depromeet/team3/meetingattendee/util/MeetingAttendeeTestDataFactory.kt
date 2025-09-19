package org.depromeet.team3.meetingattendee.util

import org.depromeet.team3.meetingattendee.MeetingAttendee

/**
 * MeetingAttendee 관련 테스트 데이터 팩토리 클래스
 */
object MeetingAttendeeTestDataFactory {

    fun createMeetingAttendee(
        id: Long = 1L,
        meetingId: Long = 1L,
        userId: Long = 1L,
        meetingNickname: String = "테스트참가자"
    ): MeetingAttendee {
        return MeetingAttendee(
            id = id,
            meetingId = meetingId,
            userId = userId,
            meetingNickname = meetingNickname
        )
    }
}
