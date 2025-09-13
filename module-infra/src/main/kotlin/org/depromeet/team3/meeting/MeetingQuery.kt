package org.depromeet.team3.meeting

import org.depromeet.team3.mapper.MeetingMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class MeetingQuery(
    private val meetingMapper: MeetingMapper,
    private val meetingJpaRepository: MeetingJpaRepository
) : MeetingRepository {

    override fun save(meeting: Meeting): Meeting {
        val entity = meetingMapper.toEntity(meeting)

        return meetingMapper.toDomain(meetingJpaRepository.save(entity))
    }

    override fun findMeetingsByUserId(userId: Long): List<Meeting> {
        return meetingJpaRepository.findByHostUserId(userId).map { meetingMapper.toDomain(it) }
    }

    override fun findById(id: Long): Meeting? {
        return meetingJpaRepository.findByIdOrNull(id)?.let { meetingMapper.toDomain(it) }
    }
}