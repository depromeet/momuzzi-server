package org.depromeet.team3.mapper

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingEntity
import org.depromeet.team3.station.StationJpaRepository
import org.depromeet.team3.user.UserRepository
import org.springframework.stereotype.Component

@Component
class MeetingMapper(
    private val userRepository: UserRepository,
    private val stationJpaRepository: StationJpaRepository,
    private val stationMapper: StationMapper
) : DomainMapper<Meeting, MeetingEntity> {
    
    override fun toDomain(entity: MeetingEntity): Meeting {
        return Meeting(
            id = entity.id!!,
            hostUserId = entity.hostUser.id!!,
            attendeeCount = entity.attendeeCount,
            isClosed = entity.isClosed,
            stationId = stationMapper.toDomain(entity.station).id!!,
            endAt = entity.endAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Meeting): MeetingEntity {
        val userEntity = userRepository.findById(domain.hostUserId)
            .orElseThrow { IllegalArgumentException(" user doesn't exist") }

        val stationEntity = stationJpaRepository.findById(domain.stationId)
            .orElseThrow { IllegalArgumentException(" station doesn't exist") }

        return MeetingEntity(
            id = domain.id,
            attendeeCount = domain.attendeeCount,
            isClosed = domain.isClosed,
            endAt = domain.endAt,
            hostUser = userEntity,
            station = stationEntity
        )
    }
}
