package org.depromeet.team3.mapper

import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeEntity
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.user.UserRepository
import org.springframework.stereotype.Component

@Component
class MeetingAttendeeMapper(
    private val userRepository: UserRepository,
    private val meetingJpaRepository: MeetingJpaRepository
) : DomainMapper<MeetingAttendee, MeetingAttendeeEntity> {
    
    override fun toDomain(entity: MeetingAttendeeEntity): MeetingAttendee {
        return MeetingAttendee(
            id = entity.id,
            meetingId = entity.meeting.id!!,
            userId = entity.user.id!!,
            meetingNickname = entity.meetingNickname,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: MeetingAttendee): MeetingAttendeeEntity {
        val meetingEntity = meetingJpaRepository.findById(domain.meetingId)
            .orElseThrow { IllegalArgumentException("Meeting not found with id: ${domain.meetingId}") }

        val userEntity = userRepository.findById(domain.userId)
            .orElseThrow { IllegalArgumentException("User not found with id: ${domain.userId}") }
        
        return MeetingAttendeeEntity(
            id = domain.id,
            meeting = meetingEntity,
            meetingNickname = domain.meetingNickname,
            user = userEntity
        )
    }
}
