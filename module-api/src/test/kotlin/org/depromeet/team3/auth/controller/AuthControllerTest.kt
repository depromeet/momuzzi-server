package org.depromeet.team3.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.depromeet.team3.auth.application.AuthService
import org.depromeet.team3.auth.application.response.UserProfileResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.config.TestSecurityConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * AuthController 통합 테스트 - Security 완전 우회
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestSecurityConfiguration::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var authService: AuthService

    @Test
    fun `카카오 로그인 성공 - 200 응답`() {
        // given
        val code = "test-auth-code"
        val userProfileResponse = UserProfileResponse(
            email = "test@example.com",
            nickname = "테스트사용자",
            profileImage = "https://example.com/profile.jpg"
        )
        
        whenever(authService.oAuthKakaoLoginWithCode(eq(code), any()))
            .thenReturn(userProfileResponse)

        // when & then
        mockMvc.perform(
            get("/api/v1/auth/kakao-login")
                .param("code", code)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("test@example.com"))
            .andExpect(jsonPath("$.data.nickname").value("테스트사용자"))
            .andExpect(jsonPath("$.data.profileImage").value("https://example.com/profile.jpg"))
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
    fun `카카오 로그인 실패 - 401 에러 (인증 실패)`() {
        // given
        val code = "invalid-auth-code"
        
        whenever(authService.oAuthKakaoLoginWithCode(eq(code), any()))
            .thenThrow(AuthException(ErrorCode.KAKAO_INVALID_GRANT))

        // when & then
        mockMvc.perform(
            get("/api/v1/auth/kakao-login")
                .param("code", code)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.name").value("KAKAO_INVALID_GRANT"))
            .andExpect(jsonPath("$.error.code").value("O001"))
            .andExpect(jsonPath("$.error.message").value("카카오 인증 코드가 유효하지 않습니다."))
    }

    @Test
    fun `토큰 재발급 성공 - 200 응답`() {
        // given
        val refreshResult = mapOf(
            "message" to "토큰이 성공적으로 갱신되었습니다",
            "status" to "success"
        )
        
        whenever(authService.refreshTokens(any(), any()))
            .thenReturn(refreshResult)

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.message").value("토큰이 성공적으로 갱신되었습니다"))
            .andExpect(jsonPath("$.data.status").value("success"))
    }

    @Test
    fun `토큰 재발급 실패 - 401 에러 (Refresh Token 없음)`() {
        // given
        val errorDetail = mapOf("reason" to "Refresh Token이 없습니다")
        
        whenever(authService.refreshTokens(any(), any()))
            .thenThrow(AuthException(ErrorCode.KAKAO_AUTH_FAILED, errorDetail))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.name").value("KAKAO_AUTH_FAILED"))
            .andExpect(jsonPath("$.error.code").value("O002"))
            .andExpect(jsonPath("$.error.message").value("카카오 인증에 실패했습니다."))
            .andExpect(jsonPath("$.error.detail.reason").value("Refresh Token이 없습니다"))
    }

    @Test
    fun `토큰 재발급 실패 - 401 에러 (Refresh Token 유효하지 않음)`() {
        // given
        val errorDetail = mapOf("reason" to "Refresh Token이 유효하지 않습니다")
        
        whenever(authService.refreshTokens(any(), any()))
            .thenThrow(AuthException(ErrorCode.KAKAO_AUTH_FAILED, errorDetail))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.detail.reason").value("Refresh Token이 유효하지 않습니다"))
    }

    @Test
    fun `서버 내부 오류 - 500 에러`() {
        // given
        whenever(authService.refreshTokens(any(), any()))
            .thenThrow(RuntimeException("서버 오류"))

        // when & then
        mockMvc.perform(
            post("/api/v1/auth/reissue-token")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.name").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.error.code").value("S001"))
    }
}
