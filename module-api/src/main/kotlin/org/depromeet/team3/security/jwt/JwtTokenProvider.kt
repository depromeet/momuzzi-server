package org.depromeet.team3.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.util.CookieUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val cookieUtil: CookieUtil,
    private val jwtProperties: JwtProperties
) {

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * 카카오 로그인용 Access Token 생성
     */
    fun generateAccessToken(
        kakaoUserId: Long,
        email: String? = null,
        authorities: Collection<String> = listOf("ROLE_USER")
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.accessTokenExpiration)

        return Jwts.builder()
            .subject(kakaoUserId.toString())
            .claim("authorities", authorities.joinToString(","))
            .claim("tokenType", "ACCESS")
            .apply {
                email?.let { claim("email", it) }
            }
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * 카카오 로그인용 Refresh Token 생성
     */
    fun generateRefreshToken(kakaoUserId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.refreshTokenExpiration)

        return Jwts.builder()
            .subject(kakaoUserId.toString())
            .claim("tokenType", "REFRESH")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * 카카오 로그인 성공 시 토큰 쿠키 설정
     */
    fun setTokenCookies(
        response: HttpServletResponse,
        kakaoUserId: Long,
        email: String? = null
    ) {
        val accessToken = generateAccessToken(kakaoUserId, email)
        val refreshToken = generateRefreshToken(kakaoUserId)

        cookieUtil.createAccessTokenCookie(response, accessToken)
        cookieUtil.createRefreshTokenCookie(response, refreshToken)
    }

    /**
     * 요청에서 토큰 추출 (쿠키 우선, 없으면 Authorization 헤더에서)
     */
    fun extractToken(request: HttpServletRequest): String? {
        // 1. 쿠키에서 토큰 확인
        val tokenFromCookie = CookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)
        if (tokenFromCookie != null) {
            return tokenFromCookie
        }

        // 2. Authorization 헤더에서 토큰 확인
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (카카오 사용자 ID)
     */
    fun getUserIdFromToken(token: String): String? {
        return try {
            val claims = getClaims(token)
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    fun getEmailFromToken(token: String): String? {
        return try {
            val claims = getClaims(token)
            claims.get("email", String::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JWT 토큰에서 권한 정보 추출
     */
    fun getAuthoritiesFromToken(token: String): Collection<String> {
        return try {
            val claims = getClaims(token)
            val authoritiesString = claims.get("authorities", String::class.java)
            authoritiesString?.split(",") ?: listOf("ROLE_USER")
        } catch (e: Exception) {
            listOf("ROLE_USER")
        }
    }

    /**
     * 토큰 타입 확인 (ACCESS 또는 REFRESH)
     */
    fun getTokenType(token: String): String? {
        return try {
            val claims = getClaims(token)
            claims.get("tokenType", String::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JWT 토큰 유효성 검증
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            // 토큰이 만료되지 않았는지 확인
            claims.expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Access Token 유효성 검증
     */
    fun validateAccessToken(token: String): Boolean {
        return validateToken(token) && getTokenType(token) == "ACCESS"
    }

    /**
     * Refresh Token 유효성 검증
     */
    fun validateRefreshToken(token: String): Boolean {
        return validateToken(token) && getTokenType(token) == "REFRESH"
    }

    /**
     * JWT 토큰에서 Authentication 객체 생성
     */
    fun getAuthentication(token: String): Authentication? {
        val userId = getUserIdFromToken(token) ?: return null
        val authorities = getAuthoritiesFromToken(token)
            .map { SimpleGrantedAuthority(it) }
        
        val principal = User(userId, "", authorities)
        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    /**
     * Refresh Token으로 새로운 Access Token 생성
     */
    fun refreshAccessToken(refreshToken: String): String? {
        return if (validateRefreshToken(refreshToken)) {
            val userId = getUserIdFromToken(refreshToken)?.toLongOrNull()
            if (userId != null) {
                generateAccessToken(userId)
            } else null
        } else null
    }

    /**
     * 로그아웃 시 쿠키에서 토큰 제거
     */
    fun clearTokenCookies(response: HttpServletResponse) {
        cookieUtil.deleteAllAuthCookies(response)
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
