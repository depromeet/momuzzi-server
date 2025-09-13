package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.MeetingAttendee
import org.depromeet.team3.meeting.MeetingAttendeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JoinMeetingService(
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(
        meetingId: Long,
        userId: Long
    ): Unit {
        val meetingAttendee = MeetingAttendee(
            null,
            meetingId,
            userId,
            null, null
        )

        meetingAttendeeRepository.save(meetingAttendee)
    }
}