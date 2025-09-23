package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JoinMeetingService(
    private val meetingRepository: MeetingRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {

    @Transactional
    operator fun invoke(
        userId: Long,
        meetingId: Long,
        attendeeNickname: String
    ): Unit {
        validateMeeting(meetingId, userId)

        val meetingAttendee = MeetingAttendee(
            null,
            meetingId,
            userId,
            attendeeNickname,
            null, null
        )

        meetingAttendeeRepository.save(meetingAttendee)
    }

    private fun validateMeeting(meetingId: Long, userId: Long) {
        val meeting = meetingRepository.findById(meetingId)
            ?: throw IllegalArgumentException("Not Found meeting ID: $meetingId")

        if (meeting.isClosed) {
            throw IllegalStateException("Ended meeting ID: $meetingId")
        }

        meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, userId)?.let {
            throw IllegalStateException("User already joined meeting ID: $meetingId")
        }

        val currentAttendeeCount = meetingAttendeeRepository.countByMeetingId(meetingId)
        if (currentAttendeeCount >= meeting.attendeeCount) {
            throw IllegalStateException("Meeting is full. Current: $currentAttendeeCount, Max: ${meeting.attendeeCount}")
        }
    }
}