package org.depromeet.team3.mapper

import org.depromeet.team3.auth.User
import org.depromeet.team3.auth.UserEntity
import org.springframework.stereotype.Component

@Component
class UserMapper : DomainMapper<User, UserEntity> {

    override fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            kakaoId = entity.kakaoId,
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
            kakaoId = domain.kakaoId,
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
