package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.User
import org.depromeet.team3.auth.UserCommandRepository
import org.depromeet.team3.auth.UserQueryRepository
import org.depromeet.team3.auth.client.KakaoOAuthClient
import org.depromeet.team3.auth.command.KakaoLoginCommand
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.dto.UserProfileResponse
import org.depromeet.team3.auth.properties.KakaoProperties
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 카카오 로그인 Command Service
 * 쓰기 작업 수행
 */
@Service
class KakaoLoginService(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val userQueryRepository: UserQueryRepository,
    private val userCommandRepository: UserCommandRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val kakaoProperties: KakaoProperties
) {

    @Transactional
    fun login(command: KakaoLoginCommand): LoginResponse {
        val code = command.authorizationCode
        
        // 1. 카카오 OAuth 토큰 요청 및 프로필 조회
        val oAuthToken = kakaoOAuthClient.requestToken(code, kakaoProperties.redirectUri)
        val kakaoProfile = kakaoOAuthClient.requestProfile(oAuthToken)

        // 2. 카카오 프로필 정보 추출
        val socialId = kakaoProfile.id.toString()
        val email = kakaoProfile.kakao_account.email
        val nickname = kakaoProfile.kakao_account.profile.nickname
        val profileImage = kakaoProfile.kakao_account.profile.profile_image_url

        // 3. 사용자 조회 또는 생성 (Query Repository로 조회, Command Repository로 저장)
        val user = findOrCreateUser(email, nickname, profileImage, socialId)

        // 4. JWT 토큰 생성 및 DB 저장
        val tokens = generateAuthenticationTokens(user)

        return LoginResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            userProfile = UserProfileResponse(
                email = user.email,
                nickname = user.nickname,
                profileImage = user.profileImage
            )
        )
    }

    /**
     * 사용자 조회 또는 신규 생성
     * Query Repository로 조회, Command Repository로 저장
     */
    private fun findOrCreateUser(
        email: String,
        nickname: String,
        profileImage: String?,
        socialId: String
    ): User {
        val existingUser = userQueryRepository.findByEmail(email)
        
        return if (existingUser != null) {
            // 기존 사용자의 프로필 이미지 업데이트
            val updatedUser = existingUser.copy(
                profileImage = profileImage,
                updatedAt = LocalDateTime.now()
            )
            userCommandRepository.save(updatedUser)
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
            kakaoId = socialId,  // kakaoId 추가
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
    private fun generateAuthenticationTokens(user: User): AuthTokens {
        val userId = user.id!!
        
        // 토큰 생성
        val accessToken = jwtTokenProvider.generateAccessToken(userId, user.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userId)
        
        // Refresh Token을 DB에 저장
        val updatedUser = user.copy(
            refreshToken = refreshToken,
            updatedAt = LocalDateTime.now()
        )
        userCommandRepository.save(updatedUser)
        
        return AuthTokens(accessToken, refreshToken)
    }
    
    private data class AuthTokens(
        val accessToken: String,
        val refreshToken: String
    )
}
