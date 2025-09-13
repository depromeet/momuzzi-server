package org.depromeet.team3.auth.application

import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.auth.KakaoOAuthClient
import org.depromeet.team3.auth.application.response.UserResponse
import org.depromeet.team3.auth.model.KakaoResponse
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.depromeet.team3.user.UserEntity
import org.depromeet.team3.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider
){

    @Value("\${kakao.redirect-uri}")
    private val kakaoRedirectUri: String? = null

    @Transactional
    fun oAuthKakaoLoginWithCode(
        code: String,
        response: HttpServletResponse
    ): UserResponse {
        // 1. 인가코드로 액세스 토큰 교환
        val oAuthToken = kakaoOAuthClient.requestToken(code, kakaoRedirectUri!!)
        
        // 2. 액세스 토큰으로 프로필 요청
        val kakaoProfile = kakaoOAuthClient.requestProfile(oAuthToken)

        val socialId = kakaoProfile.id.toString()
        val email = kakaoProfile.kakao_account.email
        val nickname = kakaoProfile.kakao_account.profile.nickname
        val profileImage = kakaoProfile.kakao_account.profile.profile_image_url

        val userEntity = userRepository.findByEmail(email)
            ?.also { existingUser ->
                // 프로필 이미지 업데이트
                existingUser.profileImage = profileImage
            }
            ?: createNewUserEntity(email, nickname, profileImage, socialId)

        // JWT 토큰을 쿠키로 설정
        jwtTokenProvider.setTokenCookies(response, userEntity.id!!, email)

        // 응답용 토큰 생성
        val accessToken = jwtTokenProvider.generateAccessToken(userEntity.id!!, email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userEntity.id!!)

        // Refresh Token 저장
        userEntity.refreshToken = refreshToken
        userRepository.save(userEntity)

        return UserResponse(
            email = userEntity.email,
            nickname = userEntity.nickname,
            profileImage = userEntity.profileImage,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    @Transactional
    fun oAuthKakaoLoginWithAccessToken(
        kakaoAccessToken: String,
        response: HttpServletResponse
    ): UserResponse {

        // KakaoOAuthClient를 통해 프로필 요청 (accessToken 사용)
        val kakaoProfile = requestProfileWithAccessToken(kakaoAccessToken)

        val socialId = kakaoProfile.id.toString()
        val email = kakaoProfile.kakao_account.email
        val nickname = kakaoProfile.kakao_account.profile.nickname
        val profileImage = kakaoProfile.kakao_account.profile.profile_image_url

        val userEntity = userRepository.findByEmail(email)
            ?.also { existingUser ->
                // 프로필 이미지 업데이트
                existingUser.profileImage = profileImage
            }
            ?: createNewUserEntity(email, nickname, profileImage, socialId)

        // JWT 토큰을 쿠키로 설정
        jwtTokenProvider.setTokenCookies(response, userEntity.id!!, email)

        // 응답용 토큰 생성
        val accessToken = jwtTokenProvider.generateAccessToken(userEntity.id!!, email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userEntity.id!!)

        // Refresh Token 저장
        userEntity.refreshToken = refreshToken
        userRepository.save(userEntity)

        return UserResponse(
            email = userEntity.email,
            nickname = userEntity.nickname,
            profileImage = userEntity.profileImage,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun testKakaoToken(code: String, redirectUri: String): String {
        return try {
            val oAuthToken = kakaoOAuthClient.requestToken(code, redirectUri)
            "토큰 요청 성공: ${oAuthToken.access_token?.substring(0, 20)}..."
        } catch (e: Exception) {
            "토큰 요청 실패: ${e.message}"
        }
    }

    private fun requestProfileWithAccessToken(accessToken: String): KakaoResponse.KakaoProfile {
        // 임시 OAuthToken 객체 생성
        val oAuthToken = KakaoResponse.OAuthToken(
            access_token = accessToken,
            token_type = "bearer",
            refresh_token = null,
            expires_in = null,
            scope = null,
            refresh_token_expires_in = null
        )
        return kakaoOAuthClient.requestProfile(oAuthToken)
    }

    private fun createNewUserEntity(
        email: String,
        nickname: String,
        profileImage: String?,
        socialId: String
    ): UserEntity {
        val newUserEntity = UserEntity().apply {
            this.email = email
            this.nickname = nickname
            this.profileImage = profileImage
            this.socialId = socialId
            this.refreshToken = null
        }
        return userRepository.save(newUserEntity)
    }
}
