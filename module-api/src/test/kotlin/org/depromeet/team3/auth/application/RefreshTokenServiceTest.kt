package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.User
import org.depromeet.team3.auth.UserCommandRepository
import org.depromeet.team3.auth.UserQueryRepository
import org.depromeet.team3.auth.command.RefreshTokenCommand
import org.depromeet.team3.auth.dto.TokenResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.auth.util.TestDataFactory
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RefreshTokenServiceTest {

    @Mock
    private lateinit var userQueryRepository: UserQueryRepository

    @Mock
    private lateinit var userCommandRepository: UserCommandRepository

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var refreshTokenService: RefreshTokenService
    
    @BeforeEach
    fun setUp() {
        refreshTokenService = RefreshTokenService(
            userQueryRepository,
            userCommandRepository,
            jwtTokenProvider
        )
    }

    @Test
    fun `토큰 갱신 실패 - 유효하지 않은 Refresh Token`() {
        // given
        val command = RefreshTokenCommand(refreshToken = "invalid-refresh-token")
        
        whenever(jwtTokenProvider.validateRefreshToken(command.refreshToken))
            .thenReturn(false)

        // when & then
        val exception = assertThrows<AuthException> {
            refreshTokenService.refresh(command)
        }
        
        assertThat(exception.detail).containsEntry("reason", "Refresh Token이 유효하지 않습니다")
    }

    @Test
    fun `토큰 갱신 실패 - 사용자 정보 없음`() {
        // given
        val command = RefreshTokenCommand(refreshToken = "valid-refresh-token")
        
        whenever(jwtTokenProvider.validateRefreshToken(command.refreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(command.refreshToken))
            .thenReturn(null)

        // when & then
        val exception = assertThrows<AuthException> {
            refreshTokenService.refresh(command)
        }
        
        assertThat(exception.detail).containsEntry("reason", "사용자 정보를 찾을 수 없습니다")
    }

    @Test
    fun `토큰 갱신 실패 - 사용자를 찾을 수 없음`() {
        // given
        val command = RefreshTokenCommand(refreshToken = "valid-refresh-token")
        val userId = 1L
        
        whenever(jwtTokenProvider.validateRefreshToken(command.refreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(command.refreshToken))
            .thenReturn(userId.toString())
        whenever(userQueryRepository.findById(userId))
            .thenReturn(null)

        // when & then
        val exception = assertThrows<AuthException> {
            refreshTokenService.refresh(command)
        }
        
        assertThat(exception.detail).containsEntry("reason", "사용자를 찾을 수 없습니다")
    }

    @Test
    fun `토큰 갱신 실패 - Refresh Token 불일치`() {
        // given
        val command = RefreshTokenCommand(refreshToken = "valid-refresh-token")
        val differentRefreshToken = "different-refresh-token"
        val userId = 1L
        
        val userWithDifferentToken = TestDataFactory.createUser(
            id = userId,
            refreshToken = differentRefreshToken
        )
        
        whenever(jwtTokenProvider.validateRefreshToken(command.refreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(command.refreshToken))
            .thenReturn(userId.toString())
        whenever(userQueryRepository.findById(userId))
            .thenReturn(userWithDifferentToken)

        // when & then
        val exception = assertThrows<AuthException> {
            refreshTokenService.refresh(command)
        }
        
        assertThat(exception.detail).containsEntry("reason", "Refresh Token이 일치하지 않습니다")
    }

    @Test
    fun `토큰 갱신 성공`() {
        // given
        val command = RefreshTokenCommand(refreshToken = "valid-refresh-token")
        val userId = 1L
        
        val userWithValidToken = TestDataFactory.createUser(
            id = userId,
            email = "test@example.com",
            refreshToken = command.refreshToken
        )
        
        val updatedUser = userWithValidToken.copy(
            refreshToken = "new-refresh-token",
            updatedAt = LocalDateTime.now()
        )
        
        whenever(jwtTokenProvider.validateRefreshToken(command.refreshToken))
            .thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(command.refreshToken))
            .thenReturn(userId.toString())
        whenever(userQueryRepository.findById(userId))
            .thenReturn(userWithValidToken)
        whenever(jwtTokenProvider.generateAccessToken(userId, "test@example.com"))
            .thenReturn("new-access-token")
        whenever(jwtTokenProvider.generateRefreshToken(userId))
            .thenReturn("new-refresh-token")
        whenever(userCommandRepository.save(any<User>()))
            .thenReturn(updatedUser)

        // when
        val result = refreshTokenService.refresh(command)

        // then
        assertThat(result).isInstanceOf(TokenResponse::class.java)
        assertThat(result.accessToken).isEqualTo("new-access-token")
        assertThat(result.refreshToken).isEqualTo("new-refresh-token")
        
        verify(jwtTokenProvider).generateAccessToken(userId, "test@example.com")
        verify(jwtTokenProvider).generateRefreshToken(userId)
        verify(userCommandRepository).save(any<User>())
    }
}
