package org.depromeet.team3.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.depromeet.team3.auth.application.KakaoOAuthService
import org.depromeet.team3.auth.application.UpdateTokenService
import org.depromeet.team3.auth.command.KakaoLoginCommand
import org.depromeet.team3.auth.command.RefreshTokenCommand
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
    private val kakaoOAuthService: KakaoOAuthService,
    private val updateTokenService: UpdateTokenService
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
        @Parameter(
            description = "카카오 OAuth 인가코드",
            required = true,
        )
        @RequestParam("code") code: String,
        
        @Parameter(
            description = "리다이렉트 URI",
            required = true,
            schema = Schema(
                type = "string",
                allowableValues = [
                    "http://localhost:3000/auth/callback",
                    "http://localhost:8080/auth/callback", 
                    "https://www.momuzzi.site/auth/callback",
                    "https://api.momuzzi.site/auth/callback"
                ]
            )
        )
        @RequestParam(value = "redirect_uri", required = false) redirectUri: String?
    ): DpmApiResponse<LoginResponse> {
        val command = KakaoLoginCommand(authorizationCode = code, redirectUri = redirectUri)
        val result = kakaoOAuthService.login(command)
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
        val command = RefreshTokenCommand(request.refreshToken)
        val result = updateTokenService.refresh(command)
        return DpmApiResponse.ok(result)
    }
}
