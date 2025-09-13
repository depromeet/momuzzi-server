package org.depromeet.team3.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class CookieUtil {

    @Value("\${spring.profiles.active:local}")
    private lateinit var activeProfile: String

    companion object {
        const val ACCESS_TOKEN_COOKIE_NAME = "accessToken"
        const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
        const val ACCESS_TOKEN_MAX_AGE = 60 * 60     // 1시간 (초 단위)
        const val REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60    // 7일 (초 단위)
        const val COOKIE_PATH = "/"

        /**
         * 특정 이름을 가진 쿠키의 값 추출 (주로 accessToken)
         */
        @JvmStatic
        fun getCookieValue(request: HttpServletRequest, name: String): String? {
            return Optional.ofNullable(request.cookies)
                .flatMap { cookies ->
                    cookies.asSequence()
                        .filter { cookie -> cookie.name == name }
                        .map { it.value }
                        .firstOrNull()
                        ?.let { Optional.of(it) } ?: Optional.empty()
                }
                .orElse(null)
        }
    }

    /**
     * 로컬 환경 여부 확인
     */
    private fun isLocal(): Boolean {
        return activeProfile == "local" || activeProfile == "prod"
    }

    /**
     * 쿠키 추가
     */
    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val isLocal = isLocal()

        val cookie = Cookie(name, value)
        cookie.path = COOKIE_PATH
        cookie.isHttpOnly = true
        cookie.secure = !isLocal
        cookie.maxAge = maxAge

        if (!isLocal) {
            cookie.setAttribute("SameSite", "None") // 운영 환경에서는 크로스사이트 요청 가능하도록 설정
        } else {
            cookie.setAttribute("SameSite", "Lax") // 로컬에서는 기본값 설정
        }

        response.addCookie(cookie)
    }

    /**
     * 쿠키를 직접 삭제하는 것이 아닌, 빈 쿠키를 덮어써서 삭제한다.
     */
    fun deleteCookie(response: HttpServletResponse, name: String) {
        val isLocal = isLocal()

        val cookie = Cookie(name, null)
        cookie.path = COOKIE_PATH
        cookie.isHttpOnly = true
        cookie.maxAge = 0
        cookie.secure = !isLocal

        if (!isLocal) {
            cookie.setAttribute("SameSite", "None")
        } else {
            cookie.setAttribute("SameSite", "Lax")
        }

        response.addCookie(cookie)
    }

    /**
     * Access Token 쿠키 생성
     */
    fun createAccessTokenCookie(response: HttpServletResponse, accessToken: String) {
        addCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_MAX_AGE)
    }

    /**
     * Refresh Token 쿠키 생성
     */
    fun createRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, REFRESH_TOKEN_MAX_AGE)
    }

    /**
     * Access Token 쿠키에서 토큰 값 가져오기
     */
    fun getAccessTokenFromCookie(request: HttpServletRequest): String? {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME)
    }

    /**
     * Refresh Token 쿠키에서 토큰 값 가져오기
     */
    fun getRefreshTokenFromCookie(request: HttpServletRequest): String? {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME)
    }

    /**
     * Access Token 쿠키 삭제
     */
    fun deleteAccessTokenCookie(response: HttpServletResponse) {
        deleteCookie(response, ACCESS_TOKEN_COOKIE_NAME)
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    fun deleteRefreshTokenCookie(response: HttpServletResponse) {
        deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
    }

    /**
     * 모든 인증 관련 쿠키 삭제
     */
    fun deleteAllAuthCookies(response: HttpServletResponse) {
        deleteAccessTokenCookie(response)
        deleteRefreshTokenCookie(response)
    }
}
