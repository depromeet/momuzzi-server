package org.depromeet.team3.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.security.util.CookieUtil
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
     * Access Token 생성
     */
    fun generateAccessToken(
        userId: Long,
        email: String? = null,
        authorities: Collection<String> = listOf("ROLE_USER")
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.accessTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
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
     * Refresh Token 생성
     */
    fun generateRefreshToken(userId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.refreshTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("tokenType", "REFRESH")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * 로그인 성공 시 토큰 쿠키 설정
     */
    fun setTokenCookies(
        response: HttpServletResponse,
        userId: Long,
        email: String? = null
    ) {
        val accessToken = generateAccessToken(userId, email)
        val refreshToken = generateRefreshToken(userId)

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

        // 2. Authorization 헤더에서 토큰 확인 (API 호출 지원)
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }

    /**
     * 요청에서 Refresh Token 추출 (쿠키에서)
     */
    fun extractRefreshToken(request: HttpServletRequest): String? {
        return CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
    }

    /**
     * Refresh Token 유효성 검증
     */
    fun validateRefreshToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            val tokenType = claims.get("tokenType", String::class.java)
            tokenType == "REFRESH" && claims.expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
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
     * Access Token 유효성 검증
     */
    fun validateAccessToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            val tokenType = claims.get("tokenType", String::class.java)
            tokenType == "ACCESS" && claims.expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    // === 추후 확장용 메서드들 ===

    /**
     * 토큰에서 이메일 추출 (추후 사용 예정)
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
     * Refresh Token 기능 (추후 확장용)
     */
    fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val claims = getClaims(refreshToken)
            val tokenType = claims.get("tokenType", String::class.java)
            
            if (tokenType == "REFRESH" && claims.expiration.after(Date())) {
                val userId = claims.subject.toLongOrNull()
                userId?.let { generateAccessToken(it) }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 로그아웃 시 쿠키 제거 (추후 로그아웃 기능용)
     */
    fun clearTokenCookies(response: HttpServletResponse) {
        cookieUtil.deleteAllAuthCookies(response)
    }

    /**
     * Authentication 객체 생성 (추후 권한 관리 확장용)
     */
    fun getAuthentication(token: String): Authentication? {
        val userId = getUserIdFromToken(token) ?: return null
        val authorities = getAuthoritiesFromToken(token).map { SimpleGrantedAuthority(it) }
        
        val principal = User(userId, "", authorities)
        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    private fun getAuthoritiesFromToken(token: String): Collection<String> {
        return try {
            val claims = getClaims(token)
            val authoritiesString = claims.get("authorities", String::class.java)
            authoritiesString?.split(",") ?: listOf("ROLE_USER")
        } catch (e: Exception) {
            listOf("ROLE_USER")
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
