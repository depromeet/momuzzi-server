package org.depromeet.team3.auth.presentation

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.auth.application.AuthService
import org.depromeet.team3.auth.application.response.UserProfileResponse
import org.depromeet.team3.common.response.DpmApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @Operation(
        summary = "카카오 소셜 로그인 API",
        description = "카카오 OAuth 인가코드로 로그인을 처리합니다. 성공 시 자동으로 accessToken, refreshToken이 쿠키로 설정됩니다." +
                "응답 바디에는 사용자 프로필 정보만 포함됩니다."
    )
    @GetMapping("/kakao-login")
    fun kakaoLogin(
        @RequestParam("code") code: String,
        response: HttpServletResponse
    ): DpmApiResponse<UserProfileResponse> {
        val result = authService.oAuthKakaoLoginWithCode(code, response)
        return DpmApiResponse.ok(result)
    }

    @Operation(
        summary = "토큰 갱신 API",
        description = "쿠키의 refreshToken을 사용하여 만료된 accessToken을 갱신합니다. " +
                "401 에러 발생 시에 수동으로 호출하여 토큰 갱신 => 성공 시 새로운 토큰들이 자동으로 쿠키에 설정됩니다"
    )
    @PostMapping("/reissue-token")
    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): DpmApiResponse<Map<String, String>> {
        val result = authService.refreshTokens(request, response)
        return DpmApiResponse.ok(result)
    }
}
