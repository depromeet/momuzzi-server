package org.depromeet.team3.common.util

import org.depromeet.team3.security.jwt.JwtAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * 테스트에서 인증 관련 작업을 도와주는 유틸리티 클래스
 */
object TestAuthHelper {

    /**
     * 테스트용 JWT 인증 토큰을 SecurityContext에 설정
     * @param userId 설정할 사용자 ID
     */
    fun setAuthenticatedUser(userId: Long) {
        val jwtToken = JwtAuthenticationToken(userId, emptyList())
        SecurityContextHolder.getContext().authentication = jwtToken
    }

    /**
     * SecurityContext를 초기화
     */
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    /**
     * 현재 인증된 사용자 ID를 반환
     * @return 현재 인증된 사용자 ID, 인증되지 않은 경우 null
     */
    fun getCurrentUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication is JwtAuthenticationToken) {
            authentication.getUserId()
        } else {
            null
        }
    }
}
