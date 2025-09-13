package org.depromeet.team3.mapper

import org.depromeet.team3.station.Station
import org.depromeet.team3.station.StationEntity
import org.springframework.stereotype.Component

@Component
class StationMapper : DomainMapper<Station, StationEntity> {
    
    override fun toDomain(entity: StationEntity): Station {
        return Station(
            id = entity.id,
            meetingId = entity.meetingId,
            name = entity.name,
            locX = entity.locX,
            locY = entity.locY,
            isDeleted = entity.isDeleted,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Station): StationEntity {
        val entity = StationEntity(
            id = domain.id,
            meetingId = domain.meetingId,
            name = domain.name,
            locX = domain.locX,
            locY = domain.locY,
            isDeleted = domain.isDeleted
        )
        // BaseTimeEntity의 createdAt은 자동으로 설정되므로 별도 설정 불필요
        // updatedAt은 필요시 updateTimestamp() 메서드 호출
        return entity
    }
}
