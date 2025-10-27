package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.User
import org.depromeet.team3.auth.UserCommandRepository
import org.depromeet.team3.auth.UserQueryRepository
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.dto.UserProfileResponse
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 카카오 로그인 Save Service
 * DB 작업만 담당하고 @Transactional 적용
 */
@Service
class CreateKakaoUserService(
    private val userQueryRepository: UserQueryRepository,
    private val userCommandRepository: UserCommandRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional(rollbackFor = [Exception::class])
    fun saveUserAndGenerateTokens(
        email: String,
        nickname: String,
        profileImage: String?,
        socialId: String
    ): LoginResponse {

        // 1. 사용자 조회 또는 생성
        val user = findOrCreateUser(email, nickname, profileImage, socialId)

        // 2. JWT 토큰 생성 및 변경사항 DB 저장
        val tokens = generateAuthenticationTokens(user, profileImage)

        return LoginResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            userProfile = UserProfileResponse(
                email = user.email,
                nickname = user.nickname,
                profileImage = tokens.updatedUserProfile?.profileImage
            )
        )
    }

    /**
     * 사용자 조회 또는 신규 생성
     */
    private fun findOrCreateUser(
        email: String,
        nickname: String,
        profileImage: String?,
        socialId: String
    ): User {
        val existingUser = userQueryRepository.findByEmail(email)

        return if (existingUser != null) {
            // 기존 사용자 반환 (저장은 토큰 생성 시 한 번에 처리)
            existingUser
        } else {
            // 신규 사용자 생성
            createNewUser(email, nickname, profileImage, socialId)
        }
    }

    /**
     * 신규 사용자 생성
     */
    private fun createNewUser(
        email: String,
        nickname: String,
        profileImage: String?,
        socialId: String
    ): User {
        val newUser = User(
            id = null,
            kakaoId = socialId,
            socialId = socialId,
            email = email,
            profileImage = profileImage,
            refreshToken = null,
            nickname = nickname,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )
        return userCommandRepository.save(newUser)
    }

    /**
     * JWT 토큰 생성 및 DB 저장
     */
    private fun generateAuthenticationTokens(user: User, newProfileImage: String?): AuthTokens {
        val userId = user.id!!

        // 토큰 생성
        val accessToken = jwtTokenProvider.generateAccessToken(userId, user.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userId)

        // 프로필 이미지가 변경되었는지 확인
        val shouldUpdateProfile = newProfileImage != null && newProfileImage != user.profileImage
        val base = if (shouldUpdateProfile) user.copy(profileImage = newProfileImage) else user
        val updatedUser = base.copy(
            refreshToken = refreshToken,
            updatedAt = LocalDateTime.now()
        )

        userCommandRepository.save(updatedUser)

        return AuthTokens(accessToken, refreshToken, updatedUser)
    }

    private data class AuthTokens(
        val accessToken: String,
        val refreshToken: String,
        val updatedUserProfile: User? = null
    )
}

