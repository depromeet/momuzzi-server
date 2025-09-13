package org.depromeet.team3.mapper

import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.user.User
import org.depromeet.team3.user.UserEntity
import org.springframework.stereotype.Component

@Component
class UserMapper(
    private val meetingJpaRepository: MeetingJpaRepository,
    private val meetingMapper: MeetingMapper
) : DomainMapper<User, UserEntity> {
    
    override fun toDomain(entity: UserEntity): User {
        val meetings = entity.meetings.map { meetingMapper.toDomain(it) }.toMutableList()

        return User(
            id = entity.id,
            kakaoId = entity.kakaoId,
            email = entity.email,
            nickname = entity.nickname,
            meetings = meetings,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: User): UserEntity {
        val meetingEntities = domain.id?.let(meetingJpaRepository::findByHostUserId) ?: emptyList()

        return UserEntity(
            id = domain.id,
            kakaoId = domain.kakaoId,
            email = domain.email,
            nickname = domain.nickname,
            meetings = meetingEntities.toMutableList()
        )
    }
}
