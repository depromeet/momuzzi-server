package org.depromeet.team3.mapper

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingEntity
import org.springframework.stereotype.Component

@Component
class MeetingMapper : DomainMapper<Meeting, MeetingEntity> {
    
    override fun toDomain(entity: MeetingEntity): Meeting {
        return Meeting(
            id = entity.id,
            hostUserId = entity.hostUserId,
            attendeeCount = entity.attendeeCount,
            isClosed = entity.isClosed,
            endAt = entity.endAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Meeting): MeetingEntity {
        val entity = MeetingEntity(
            id = domain.id,
            hostUserId = domain.hostUserId,
            attendeeCount = domain.attendeeCount,
            isClosed = domain.isClosed,
            endAt = domain.endAt
        )
        // BaseTimeEntity의 createdAt은 자동으로 설정되므로 별도 설정 불필요
        // updatedAt은 필요시 updateTimestamp() 메서드 호출
        return entity
    }
}
