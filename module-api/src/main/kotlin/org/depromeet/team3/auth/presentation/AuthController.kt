package org.depromeet.team3.auth.presentation

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.auth.application.AuthService
import org.depromeet.team3.auth.application.response.UserResponse
import org.depromeet.team3.common.response.DpmApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @Operation(summary = "카카오 로그인", description = "카카오 서버로부터 인가코드를 받아 이를 기반으로 로그인 처리")
    @GetMapping("/kakao-login")
    fun kakaoLogin(
        @RequestParam("code") code: String,
        response: HttpServletResponse
    ): DpmApiResponse<UserResponse> {
        val result = authService.oAuthKakaoLoginWithCode(code, response)
        return DpmApiResponse.ok(result)
    }
}
