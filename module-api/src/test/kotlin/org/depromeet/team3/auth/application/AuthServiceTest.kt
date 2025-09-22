package org.depromeet.team3.auth.application

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.auth.KakaoOAuthClient
import org.depromeet.team3.auth.KakaoProperties
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.auth.model.KakaoResponse
import org.depromeet.team3.auth.util.TestDataFactory
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.depromeet.team3.user.UserEntity
import org.depromeet.team3.user.UserRepository
import org.depromeet.team3.user.util.UserTestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.assertThat
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var kakaoOAuthClient: KakaoOAuthClient

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    private lateinit var authService: AuthService

    private lateinit var kakaoProfile: KakaoResponse.KakaoProfile
    private lateinit var oAuthToken: KakaoResponse.OAuthToken
    private lateinit var existingUser: UserEntity
    
    @BeforeEach
    fun setUp() {
        // AuthService 생성
        val kakaoProperties = KakaoProperties().apply {
            redirectUri = "http://localhost:8080/login/oauth2/code/kakao"
        }
        authService = AuthService(kakaoOAuthClient, userRepository, jwtTokenProvider, kakaoProperties)

        // 카카오 프로필 모의 데이터
        kakaoProfile = TestDataFactory.createKakaoProfile()

        oAuthToken = TestDataFactory.createOAuthToken()

        existingUser = UserTestDataFactory.createUserEntity(
            id = 1L,
            socialId = "12345",
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/old-profile.jpg"
        )
    }

    @Test
    fun `카카오 로그인 성공 - 신규 사용자`() {
        // given
        val code = "test-code"
        
        whenever(kakaoOAuthClient.requestToken(any(), any()))
            .thenReturn(oAuthToken)
        whenever(kakaoOAuthClient.requestProfile(oAuthToken))
            .thenReturn(kakaoProfile)
        whenever(userRepository.findByEmail("test@example.com"))
            .thenReturn(null)
        
        val savedUser = UserEntity(
            id = 2L,
            socialId = "12345",
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "http://example.com/profile.jpg",
            refreshToken = null
        )
        whenever(userRepository.save(any<UserEntity>()))
            .thenReturn(savedUser)
        whenever(jwtTokenProvider.generateRefreshToken(2L))
            .thenReturn("refresh-token")

        // when
        val result = authService.oAuthKakaoLoginWithCode(code, response)

        // then
        assertThat(result.email).isEqualTo("test@example.com")
        assertThat(result.nickname).isEqualTo("테스트사용자")
        assertThat(result.profileImage).isEqualTo("http://example.com/profile.jpg")

        verify(jwtTokenProvider).setTokenCookies(any(), any(), any())
    }

    @Test
    fun `토큰 갱신 실패 - Refresh Token 없음`() {
        // given
        whenever(jwtTokenProvider.extractRefreshToken(request))
            .thenReturn(null)

        // when & then
        val exception = assertThrows<AuthException> {
            authService.refreshTokens(request, response)
        }
        
        assertThat(exception.detail).containsEntry("reason", "Refresh Token이 없습니다")
    }

    @Test
    fun `토큰 갱신 실패 - 유효하지 않은 Refresh Token`() {
        // given
        val invalidRefreshToken = "invalid-refresh-token"
        
        whenever(jwtTokenProvider.extractRefreshToken(request))
            .thenReturn(invalidRefreshToken)
        whenever(jwtTokenProvider.validateRefreshToken(invalidRefreshToken))
            .thenReturn(false)

        // when & then
        val exception = assertThrows<AuthException> {
            authService.refreshTokens(request, response)
        }
        
        assertThat(exception.detail).containsEntry("reason", "Refresh Token이 유효하지 않습니다")
    }

    @Test
    fun `토큰 갱신 실패 - 사용자 정보 없음`() {
        // given
        val validRefreshToken = "valid-refresh-token"
        
        whenever(jwtTokenProvider.extractRefreshToken(request))
            .thenReturn(validRefreshToken)
        whenever(jwtTokenProvider.validateRefreshToken(validRefreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(validRefreshToken))
            .thenReturn(null)

        // when & then
        val exception = assertThrows<AuthException> {
            authService.refreshTokens(request, response)
        }
        
        assertThat(exception.detail).containsEntry("reason", "사용자 정보를 찾을 수 없습니다")
    }

    @Test
    fun `토큰 갱신 실패 - 사용자를 찾을 수 없음`() {
        // given
        val validRefreshToken = "valid-refresh-token"
        val userId = 1L
        
        whenever(jwtTokenProvider.extractRefreshToken(request))
            .thenReturn(validRefreshToken)
        whenever(jwtTokenProvider.validateRefreshToken(validRefreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(validRefreshToken))
            .thenReturn(userId.toString())
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.empty())

        // when & then
        val exception = assertThrows<AuthException> {
            authService.refreshTokens(request, response)
        }
        
        assertThat(exception.detail).containsEntry("reason", "사용자를 찾을 수 없습니다")
    }

    @Test
    fun `토큰 갱신 실패 - Refresh Token 불일치`() {
        // given
        val validRefreshToken = "valid-refresh-token"
        val differentRefreshToken = "different-refresh-token"
        val userId = 1L
        
        val userWithDifferentToken = UserTestDataFactory.createUserEntity(
            id = userId,
            refreshToken = differentRefreshToken
        )
        
        whenever(jwtTokenProvider.extractRefreshToken(request))
            .thenReturn(validRefreshToken)
        whenever(jwtTokenProvider.validateRefreshToken(validRefreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(validRefreshToken))
            .thenReturn(userId.toString())
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(userWithDifferentToken))

        // when & then
        val exception = assertThrows<AuthException> {
            authService.refreshTokens(request, response)
        }
        
        assertThat(exception.detail).containsEntry("reason", "Refresh Token이 일치하지 않습니다")
    }

    @Test
    fun `토큰 갱신 성공`() {
        // given
        val validRefreshToken = "valid-refresh-token"
        val userId = 1L
        
        val userWithValidToken = UserTestDataFactory.createUserEntity(
            id = userId,
            refreshToken = validRefreshToken
        )
        
        whenever(jwtTokenProvider.extractRefreshToken(request))
            .thenReturn(validRefreshToken)
        whenever(jwtTokenProvider.validateRefreshToken(validRefreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(validRefreshToken))
            .thenReturn(userId.toString())
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(userWithValidToken))
        whenever(jwtTokenProvider.generateRefreshToken(userId))
            .thenReturn("new-refresh-token")
        whenever(userRepository.save(any<UserEntity>()))
            .thenReturn(userWithValidToken)

        // when
        val result = authService.refreshTokens(request, response)

        // then
        assertThat(result["message"]).isEqualTo("토큰이 성공적으로 갱신되었습니다")
        assertThat(result["status"]).isEqualTo("success")
        
        verify(jwtTokenProvider).setTokenCookies(any(), any(), any())
        verify(userRepository).save(any<UserEntity>())
    }
}
