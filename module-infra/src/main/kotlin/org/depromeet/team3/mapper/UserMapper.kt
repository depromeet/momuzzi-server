package org.depromeet.team3.mapper

import org.depromeet.team3.user.User
import org.depromeet.team3.user.UserEntity
import org.springframework.stereotype.Component

@Component
class UserMapper : DomainMapper<User, UserEntity> {
    
    override fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            email = entity.email,
            nickname = entity.nickname,
            profileImage = entity.profileImage,
            socialId = entity.socialId,
            refreshToken = entity.refreshToken,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.id,
            socialId = domain.socialId,
            email = domain.email,
            nickname = domain.nickname,
            profileImage = domain.profileImage,
            refreshToken = domain.refreshToken
        ).also {
            // BaseTimeEntity의 createdAt, updatedAt 처리는 자동으로 됨
        }
    }
}
