package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.MeetingAttendee
import org.depromeet.team3.meeting.MeetingAttendeeRepository
import org.depromeet.team3.meeting.MeetingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JoinMeetingService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(
        meetingId: Long,
        userId: Long
    ): Unit {
        validateMeeting(meetingId)

        meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId)?.let {
            throw IllegalStateException("User already joined meeting ID: $meetingId")
        }

        val meetingAttendee = MeetingAttendee(
            null,
            meetingId,
            userId,
            null, null
        )

        meetingAttendeeRepository.save(meetingAttendee)
    }

    private fun validateMeeting(meetingId: Long) {
        val meeting = meetingRepository.findById(meetingId)
            ?: throw IllegalArgumentException("Not Found meeting ID: $meetingId")

        if (meeting.isClosed) {
            throw IllegalStateException("Ended meeting ID: $meetingId")
        }
    }
}