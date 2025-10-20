package org.depromeet.team3.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.depromeet.team3.auth.application.KakaoLoginService
import org.depromeet.team3.auth.application.RefreshTokenService
import org.depromeet.team3.auth.command.KakaoLoginCommand
import org.depromeet.team3.auth.command.RefreshTokenCommand
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.dto.RefreshTokenRequest
import org.depromeet.team3.auth.dto.TokenResponse
import org.depromeet.team3.auth.dto.UserProfileResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders


@ExtendWith(MockitoExtension::class)
class AuthControllerTest {

    @Mock
    private lateinit var kakaoLoginService: KakaoLoginService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @InjectMocks
    private lateinit var authController: AuthController

    private val objectMapper = ObjectMapper()
    
    private val mockMvc: MockMvc by lazy {
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `카카오 로그인 성공 - 200 응답`() {
        // given
        val code = "test-auth-code"
        val command = KakaoLoginCommand(authorizationCode = code)
        val loginResponse = LoginResponse(
            accessToken = "access-token-123",
            refreshToken = "refresh-token-456",
            userProfile = UserProfileResponse(
                email = "test@example.com",
                nickname = "테스트사용자",
                profileImage = "https://example.com/profile.jpg"
            )
        )
        
        whenever(kakaoLoginService.login(any<KakaoLoginCommand>())).thenReturn(loginResponse)

        // when & then
        mockMvc.perform(
            get("/api/v1/auth/kakao-login")
                .param("code", code)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"))
            .andExpect(jsonPath("$.data.userProfile.email").value("test@example.com"))
            .andExpect(jsonPath("$.data.userProfile.nickname").value("테스트사용자"))
            .andExpect(jsonPath("$.data.userProfile.profileImage").value("https://example.com/profile.jpg"))
    }

    @Test
    fun `카카오 로그인 실패 - 400 에러 (코드 누락)`() {
        // when & then
        mockMvc.perform(
            get("/api/v1/auth/kakao-login")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `토큰 재발급 성공 - 200 응답`() {
        // given
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = "valid-refresh-token")
        val command = RefreshTokenCommand(refreshToken = "valid-refresh-token")
        val tokenResponse = TokenResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )
        
        whenever(refreshTokenService.refresh(any<RefreshTokenCommand>())).thenReturn(tokenResponse)

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
    }

    @Test
    fun `토큰 재발급 실패 - 401 에러 (Refresh Token 유효하지 않음)`() {
        // given
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = "invalid-refresh-token")
        
        whenever(refreshTokenService.refresh(any<RefreshTokenCommand>()))
            .thenThrow(AuthException(ErrorCode.REFRESH_TOKEN_INVALID))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("J008"))
    }

    @Test
    fun `토큰 재발급 실패 - 401 에러 (토큰 사용자 ID 유효하지 않음)`() {
        // given
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = "valid-refresh-token")
        
        whenever(refreshTokenService.refresh(any<RefreshTokenCommand>()))
            .thenThrow(AuthException(ErrorCode.TOKEN_USER_ID_INVALID))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("J012"))
    }

    @Test
    fun `토큰 재발급 실패 - 404 에러 (사용자 정보 없음)`() {
        // given
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = "valid-refresh-token")
        
        whenever(refreshTokenService.refresh(any<RefreshTokenCommand>()))
            .thenThrow(AuthException(ErrorCode.USER_NOT_FOUND_FOR_TOKEN))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("J011"))
    }

    @Test
    fun `토큰 재발급 실패 - 401 에러 (Refresh Token 불일치)`() {
        // given
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = "different-refresh-token")
        
        whenever(refreshTokenService.refresh(any<RefreshTokenCommand>()))
            .thenThrow(AuthException(ErrorCode.REFRESH_TOKEN_MISMATCH))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("J010"))
    }

    @Test
    fun `서버 내부 오류 - 500 에러`() {
        // given
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = "valid-refresh-token")
        
        whenever(refreshTokenService.refresh(any<RefreshTokenCommand>()))
            .thenThrow(RuntimeException("서버 오류"))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest))
        )
            .andExpect(status().isInternalServerError)
    }
}
