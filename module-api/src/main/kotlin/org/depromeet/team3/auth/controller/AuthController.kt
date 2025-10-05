package org.depromeet.team3.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.depromeet.team3.auth.application.AuthService
import org.depromeet.team3.auth.dto.LoginResponse
import org.depromeet.team3.auth.dto.RefreshTokenRequest
import org.depromeet.team3.auth.dto.TokenResponse
import org.depromeet.team3.common.ContextConstants
import org.depromeet.team3.common.response.DpmApiResponse
import org.springframework.web.bind.annotation.*

@Tag(name = "로그인/회원가입", description = "사용자 로그인 관련 API")
@RestController
@RequestMapping("${ContextConstants.API_VERSION_V1}/auth")
class AuthController(
    private val authService: AuthService
) {
    @Operation(
        summary = "카카오 소셜 로그인 API",
        description = "카카오 OAuth 인가코드로 로그인을 처리합니다. 성공 시 응답 바디에 accessToken, refreshToken, 사용자 프로필 정보를 반환합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ApiResponse(responseCode = "401", description = "카카오 인증 실패"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    )
    @GetMapping("/kakao-login")
    fun kakaoLogin(
        @RequestParam("code") code: String
    ): DpmApiResponse<LoginResponse> {
        val result = authService.oAuthKakaoLoginWithCode(code)
        return DpmApiResponse.ok(result)
    }

    @Operation(
        summary = "토큰 갱신 API",
        description = "refreshToken을 사용하여 만료된 accessToken을 갱신합니다. " +
                "요청 바디에 refreshToken을 포함하여 호출하면 새로운 accessToken과 refreshToken을 반환합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        ApiResponse(responseCode = "401", description = "토큰 갱신 실패"),
        ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    )
    @PostMapping("/reissue-token")
    fun refreshToken(
        @RequestBody request: RefreshTokenRequest
    ): DpmApiResponse<TokenResponse> {
        val result = authService.refreshTokens(request.refreshToken)
        return DpmApiResponse.ok(result)
    }
}
