package org.depromeet.team3.mapper

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingEntity
import org.depromeet.team3.auth.UserRepository
import org.springframework.stereotype.Component

@Component
class MeetingMapper(
    private val userRepository: UserRepository
) : DomainMapper<Meeting, MeetingEntity> {
    
    override fun toDomain(entity: MeetingEntity): Meeting {
        return Meeting(
            id = entity.id!!,
            name = entity.name,
            hostUserId = entity.hostUser.id!!,
            attendeeCount = entity.attendeeCount,
            isClosed = entity.isClosed,
            stationId = entity.stationId,
            endAt = entity.endAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Meeting): MeetingEntity {
        val userEntity = userRepository.findById(domain.hostUserId)
            .orElseThrow { IllegalArgumentException(" user doesn't exist") }

        return MeetingEntity(
            id = domain.id,
            name = domain.name,
            attendeeCount = domain.attendeeCount,
            isClosed = domain.isClosed,
            endAt = domain.endAt,
            stationId = domain.stationId,
            hostUser = userEntity
        )
    }
}
