package org.depromeet.team3.mapper

import org.depromeet.team3.user.User
import org.depromeet.team3.user.UserEntity
import org.springframework.stereotype.Component

@Component
class UserMapper : DomainMapper<User, UserEntity> {
    
    override fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            kakaoId = entity.kakaoId,
            email = entity.email,
            nickname = entity.nickname,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: User): UserEntity {
        val entity = UserEntity(
            id = domain.id,
            kakaoId = domain.kakaoId,
            email = domain.email,
            nickname = domain.nickname
        )
        // BaseTimeEntity의 createdAt은 자동으로 설정되므로 별도 설정 불필요
        // updatedAt은 필요시 updateTimestamp() 메서드 호출
        return entity
    }
}
