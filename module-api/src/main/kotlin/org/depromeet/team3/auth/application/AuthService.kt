package org.depromeet.team3.auth.application

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.auth.KakaoOAuthClient
import org.depromeet.team3.auth.KakaoProperties
import org.depromeet.team3.auth.application.response.UserProfileResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.depromeet.team3.user.UserEntity
import org.depromeet.team3.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val kakaoProperties: KakaoProperties
) {

    @Transactional
    fun oAuthKakaoLoginWithCode(
        code: String,
        response: HttpServletResponse
    ): UserProfileResponse {
        // 1. 카카오 OAuth 토큰 요청 및 프로필 조회
        val oAuthToken = kakaoOAuthClient.requestToken(code, kakaoProperties.getRedirectUri())
        val kakaoProfile = kakaoOAuthClient.requestProfile(oAuthToken)

        // 2. 카카오 프로필 정보 추출
        val socialId = kakaoProfile.id.toString()
        val email = kakaoProfile.kakao_account.email
        val nickname = kakaoProfile.kakao_account.profile.nickname
        val profileImage = kakaoProfile.kakao_account.profile.profile_image_url

        // 3. 사용자 조회 또는 생성
        val userEntity = findOrCreateUser(email, nickname, profileImage, socialId)

        // 4. JWT 토큰 생성 및 쿠키 설정
        setAuthenticationTokens(response, userEntity)

        return UserProfileResponse(
            email = userEntity.email,
            nickname = userEntity.nickname,
            profileImage = userEntity.profileImage
        )
    }

    @Transactional
    fun refreshTokens(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Map<String, String> {
        // 1. Refresh Token 추출 및 검증
        val refreshToken = jwtTokenProvider.extractRefreshToken(request)
            ?: throw AuthException(ErrorCode.KAKAO_AUTH_FAILED, message = "Refresh Token이 없습니다")

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw AuthException(ErrorCode.KAKAO_AUTH_FAILED, message = "Refresh Token이 유효하지 않습니다")
        }

        // 2. 사용자 정보 조회 및 토큰 일치성 검증
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)?.toLongOrNull()
            ?: throw AuthException(ErrorCode.KAKAO_AUTH_FAILED, message = "사용자 정보를 찾을 수 없습니다")

        val userEntity = userRepository.findById(userId).orElseThrow {
            AuthException(ErrorCode.KAKAO_AUTH_FAILED, message = "사용자를 찾을 수 없습니다")
        }

        if (userEntity.refreshToken != refreshToken) {
            throw AuthException(ErrorCode.KAKAO_AUTH_FAILED, message = "Refresh Token이 일치하지 않습니다")
        }

        // 3. 새로운 토큰 생성 및 설정
        setAuthenticationTokens(response, userEntity)

        return mapOf(
            "message" to "토큰이 성공적으로 갱신되었습니다",
            "status" to "success"
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
    ): UserEntity {
        return userRepository.findByEmail(email)
            ?.apply {
                // 기존 사용자의 프로필 이미지만 업데이트
                this.profileImage = profileImage
            }
            ?: createNewUser(email, nickname, profileImage, socialId)
    }

    /**
     * 신규 사용자 생성
     */
    private fun createNewUser(
        email: String,
        nickname: String,
        profileImage: String?,
        socialId: String
    ): UserEntity {
        val newUser = UserEntity(
            socialId = socialId,
            email = email,
            profileImage = profileImage,
            refreshToken = null,
            nickname = nickname
        )
        return userRepository.save(newUser)
    }

    /**
     * JWT 토큰 생성 및 쿠키 설정, DB 저장
     */
    private fun setAuthenticationTokens(response: HttpServletResponse, userEntity: UserEntity) {
        val userId = userEntity.id!!
        
        // 토큰 생성 및 쿠키 설정
        jwtTokenProvider.setTokenCookies(response, userId, userEntity.email)
        
        // Refresh Token을 DB에 저장
        val refreshToken = jwtTokenProvider.generateRefreshToken(userId)
        userEntity.refreshToken = refreshToken
        userRepository.save(userEntity)
    }
}
