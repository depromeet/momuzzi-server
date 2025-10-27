package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.client.KakaoOAuthClient
import org.depromeet.team3.auth.command.KakaoLoginCommand
import org.depromeet.team3.auth.properties.KakaoProperties
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.model.KakaoResponse
import org.depromeet.team3.auth.util.TestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class KakaoOAuthServiceTest {

    @Mock
    private lateinit var kakaoOAuthClient: KakaoOAuthClient

    @Mock
    private lateinit var createKakaoUserService: CreateKakaoUserService

    private lateinit var kakaoOAuthService: KakaoOAuthService

    private lateinit var kakaoProfile: KakaoResponse.KakaoProfile
    private lateinit var oAuthToken: KakaoResponse.OAuthToken
    
    @BeforeEach
    fun setUp() {
        val kakaoProperties = KakaoProperties().apply {
            redirectUri = "http://localhost:8080/login/oauth2/code/kakao"
        }
        kakaoOAuthService = KakaoOAuthService(
            kakaoOAuthClient, 
            createKakaoUserService,
            kakaoProperties
        )

        kakaoProfile = TestDataFactory.createKakaoProfile()
        oAuthToken = TestDataFactory.createOAuthToken()
    }

    @Test
    fun `카카오 로그인 성공 - 신규 사용자`() {
        // given
        val command = KakaoLoginCommand(authorizationCode = "test-code")
        val loginResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            userProfile = org.depromeet.team3.auth.dto.UserProfileResponse(
                email = "test@example.com",
                nickname = "테스트사용자",
                profileImage = "http://example.com/profile.jpg"
            )
        )
        
        whenever(kakaoOAuthClient.requestToken(any(), any()))
            .thenReturn(oAuthToken)
        whenever(kakaoOAuthClient.requestProfile(oAuthToken))
            .thenReturn(kakaoProfile)
        whenever(createKakaoUserService.saveUserAndGenerateTokens(
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/profile.jpg",
            socialId = "12345"
        )).thenReturn(loginResponse)

        // when
        val result = kakaoOAuthService.login(command)

        // then
        assertThat(result).isInstanceOf(LoginResponse::class.java)
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.userProfile.email).isEqualTo("test@example.com")
        assertThat(result.userProfile.nickname).isEqualTo("테스트사용자")
        assertThat(result.userProfile.profileImage).isNotNull()
        
        verify(createKakaoUserService).saveUserAndGenerateTokens(
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/profile.jpg",
            socialId = "12345"
        )
    }

    @Test
    fun `카카오 로그인 성공 - 기존 사용자`() {
        // given
        val command = KakaoLoginCommand(authorizationCode = "test-code")
        val loginResponse = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            userProfile = org.depromeet.team3.auth.dto.UserProfileResponse(
                email = "test@example.com",
                nickname = "기존사용자",
                profileImage = "http://example.com/profile.jpg"
            )
        )
        
        whenever(kakaoOAuthClient.requestToken(any(), any()))
            .thenReturn(oAuthToken)
        whenever(kakaoOAuthClient.requestProfile(oAuthToken))
            .thenReturn(kakaoProfile)
        whenever(createKakaoUserService.saveUserAndGenerateTokens(
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/profile.jpg",
            socialId = "12345"
        )).thenReturn(loginResponse)

        // when
        val result = kakaoOAuthService.login(command)

        // then
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.userProfile.email).isEqualTo("test@example.com")
        
        verify(createKakaoUserService).saveUserAndGenerateTokens(
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/profile.jpg",
            socialId = "12345"
        )
    }
}
