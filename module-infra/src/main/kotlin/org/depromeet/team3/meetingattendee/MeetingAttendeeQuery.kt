package org.depromeet.team3.meeting

import org.depromeet.team3.mapper.MeetingAttendeeMapper
import org.springframework.stereotype.Repository

@Repository
class MeetingAttendeeQuery(
    private val meetingAttendeeMapper: MeetingAttendeeMapper,
    private val meetingAttendeeJpaRepository: MeetingAttendeeJpaRepository
) : MeetingAttendeeRepository {

    override fun save(meetingAttendee: MeetingAttendee): MeetingAttendee {
        val entity = meetingAttendeeMapper.toEntity(meetingAttendee)

        return meetingAttendeeMapper.toDomain(meetingAttendeeJpaRepository.save(entity))
    }

    override fun findByMeetingId(meetingId: Long): List<MeetingAttendee> {
        return meetingAttendeeJpaRepository.findByMeetingId(meetingId)
            .map { meetingAttendeeMapper.toDomain(it) }
    }

    override fun findByUserId(userId: Long): List<MeetingAttendee> {
        return meetingAttendeeJpaRepository.findByUserId(userId)
            .map { meetingAttendeeMapper.toDomain(it) }
    }

    override fun findByMeetingIdAndUserId(
        meetingId: Long,
        userId: Long
    ): MeetingAttendee? {
        return meetingAttendeeJpaRepository.findByMeetingIdAndUserId(meetingId, userId)
            ?.let { meetingAttendeeMapper.toDomain(it) }
    }

    override fun existsByMeetingIdAndUserId(
        meetingId: Long,
        userId: Long
    ): Boolean {
        return meetingAttendeeJpaRepository.existsByMeetingIdAndUserId(meetingId, userId)
    }

    override fun deleteById(id: Long) {
        meetingAttendeeJpaRepository.deleteById(id)
    }
}
