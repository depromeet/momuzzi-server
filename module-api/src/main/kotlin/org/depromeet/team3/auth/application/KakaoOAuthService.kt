package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.client.KakaoOAuthClient
import org.depromeet.team3.auth.command.KakaoLoginCommand
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.auth.properties.KakaoProperties
import org.depromeet.team3.common.exception.ErrorCode
import org.springframework.stereotype.Service

/**
 * 카카오 로그인 Orchestrator
 * 외부 API 호출만 담당하고, DB 작업은 SaveService에 위임
 * 트랜잭션 없음 - DB 작업만 별도 트랜잭션으로 처리
 */
@Service
class KakaoOAuthService(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val createKakaoUserService: CreateKakaoUserService,
    private val kakaoProperties: KakaoProperties
) {

    fun login(command: KakaoLoginCommand): LoginResponse {
        val code = command.authorizationCode
        val redirectUri = command.redirectUri ?: getDefaultRedirectUri()

        // 1. 카카오 OAuth 토큰 요청 및 프로필 조회
        val oAuthToken = kakaoOAuthClient.requestToken(code, redirectUri)
        val kakaoProfile = kakaoOAuthClient.requestProfile(oAuthToken)

        // 2. 카카오 프로필 정보 추출
        val socialId = kakaoProfile.id.toString()
        val email = kakaoProfile.kakao_account.email
        val nickname = kakaoProfile.kakao_account.profile.nickname
        val profileImage = kakaoProfile.kakao_account.profile.profile_image_url

        // 3. DB 작업만 SaveService에 위임 (별도 트랜잭션)
        return createKakaoUserService.saveUserAndGenerateTokens(email, nickname, profileImage, socialId)
    }
    
    /**
     * 환경에 따른 기본 redirect URI 선택
     * 우선순위: redirectUris 배열 > redirectUri
     * 설정이 없으면 AuthException 던짐
     */
    private fun getDefaultRedirectUri(): String {
        return when {
            kakaoProperties.redirectUris.isNotEmpty() -> kakaoProperties.redirectUris.first()
            !kakaoProperties.redirectUri.isNullOrBlank() -> kakaoProperties.redirectUri
            else -> throw AuthException(
                errorCode = ErrorCode.KAKAO_REDIRECT_URI_NOT_CONFIGURED
            )
        }
    }
}