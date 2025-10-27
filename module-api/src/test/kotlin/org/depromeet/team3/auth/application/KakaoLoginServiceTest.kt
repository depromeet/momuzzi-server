package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.User
import org.depromeet.team3.auth.UserCommandRepository
import org.depromeet.team3.auth.UserQueryRepository
import org.depromeet.team3.auth.client.KakaoOAuthClient
import org.depromeet.team3.auth.command.KakaoLoginCommand
import org.depromeet.team3.auth.properties.KakaoProperties
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.model.KakaoResponse
import org.depromeet.team3.auth.util.TestDataFactory
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class KakaoLoginServiceTest {

    @Mock
    private lateinit var kakaoOAuthClient: KakaoOAuthClient

    @Mock
    private lateinit var userQueryRepository: UserQueryRepository

    @Mock
    private lateinit var userCommandRepository: UserCommandRepository

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var kakaoLoginService: KakaoLoginService

    private lateinit var kakaoProfile: KakaoResponse.KakaoProfile
    private lateinit var oAuthToken: KakaoResponse.OAuthToken
    
    @BeforeEach
    fun setUp() {
        val kakaoProperties = KakaoProperties().apply {
            redirectUri = "http://localhost:8080/login/oauth2/code/kakao"
        }
        kakaoLoginService = KakaoLoginService(
            kakaoOAuthClient, 
            userQueryRepository, 
            userCommandRepository, 
            jwtTokenProvider, 
            kakaoProperties
        )

        kakaoProfile = TestDataFactory.createKakaoProfile()
        oAuthToken = TestDataFactory.createOAuthToken()
    }

    @Test
    fun `카카오 로그인 성공 - 신규 사용자`() {
        // given
        val command = KakaoLoginCommand(authorizationCode = "test-code")
        
        whenever(kakaoOAuthClient.requestToken(any(), any()))
            .thenReturn(oAuthToken)
        whenever(kakaoOAuthClient.requestProfile(oAuthToken))
            .thenReturn(kakaoProfile)
        whenever(userQueryRepository.findByEmail("test@example.com"))
            .thenReturn(null)
        
        val savedUser = User(
            id = 2L,
            kakaoId = "12345",
            socialId = "12345",
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/profile.jpg",
            refreshToken = "refresh-token",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        whenever(userCommandRepository.save(any<User>()))
            .thenReturn(savedUser)
        whenever(jwtTokenProvider.generateAccessToken(2L, "test@example.com"))
            .thenReturn("access-token")
        whenever(jwtTokenProvider.generateRefreshToken(2L))
            .thenReturn("refresh-token")

        // when
        val result = kakaoLoginService.login(command)

        // then
        assertThat(result).isInstanceOf(LoginResponse::class.java)
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.userProfile.email).isEqualTo("test@example.com")
        assertThat(result.userProfile.nickname).isEqualTo("테스트사용자")
        assertThat(result.userProfile.profileImage).isNotNull()

        verify(jwtTokenProvider).generateAccessToken(2L, "test@example.com")
        verify(jwtTokenProvider).generateRefreshToken(2L)
        verify(userCommandRepository, times(2)).save(any<User>()) // 신규 사용자: 생성 + 토큰 업데이트
    }

    @Test
    fun `카카오 로그인 성공 - 기존 사용자`() {
        // given
        val command = KakaoLoginCommand(authorizationCode = "test-code")
        
        val existingUser = User(
            id = 1L,
            kakaoId = "12345",
            socialId = "12345",
            email = "test@example.com",
            nickname = "기존사용자",
            profileImage = "http://example.com/old-profile.jpg",
            refreshToken = null,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = null
        )
        
        val updatedUser = existingUser.copy(
            profileImage = "http://example.com/profile.jpg",
            refreshToken = "refresh-token",
            updatedAt = LocalDateTime.now()
        )
        
        whenever(kakaoOAuthClient.requestToken(any(), any()))
            .thenReturn(oAuthToken)
        whenever(kakaoOAuthClient.requestProfile(oAuthToken))
            .thenReturn(kakaoProfile)
        whenever(userQueryRepository.findByEmail("test@example.com"))
            .thenReturn(existingUser)
        whenever(userCommandRepository.save(any<User>()))
            .thenReturn(updatedUser)
        whenever(jwtTokenProvider.generateAccessToken(1L, "test@example.com"))
            .thenReturn("access-token")
        whenever(jwtTokenProvider.generateRefreshToken(1L))
            .thenReturn("refresh-token")

        // when
        val result = kakaoLoginService.login(command)

        // then
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.userProfile.email).isEqualTo("test@example.com")
        
        verify(userCommandRepository, times(1)).save(any<User>())
        verify(userQueryRepository).findByEmail("test@example.com")
    }
}
