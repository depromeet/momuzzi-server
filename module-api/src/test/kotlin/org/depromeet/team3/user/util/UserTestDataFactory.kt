package org.depromeet.team3.user.util

import org.depromeet.team3.user.UserEntity

/**
 * User 관련 테스트 데이터 팩토리 클래스
 */
object UserTestDataFactory {

    fun createUserEntity(
        id: Long? = null,
        socialId: String = "test_social_id",
        email: String = "test@example.com",
        nickname: String = "테스트사용자",
        profileImage: String? = "http://example.com/profile.jpg",
        refreshToken: String? = null
    ): UserEntity {
        return UserEntity(
            id = id,
            socialId = socialId,
            email = email,
            nickname = nickname,
            profileImage = profileImage,
            refreshToken = refreshToken
        )
    }
}
