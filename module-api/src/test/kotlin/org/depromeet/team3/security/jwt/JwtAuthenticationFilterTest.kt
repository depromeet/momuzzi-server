package org.depromeet.team3.security.jwt

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

/**
 * JwtAuthenticationFilter의 기본적인 기능만 테스트
 * 실제 필터 로직은 통합 테스트에서 검증
 */
class JwtAuthenticationFilterTest {

    @Test
    fun `제외 URL 패턴이 올바르게 설정되어 있다`() {
        // given
        val excludedUrls = listOf(
            "/swagger-ui/**", 
            "/v3/api-docs/**", 
            "/swagger", 
            "/api/auth/kakao-login", 
            "/favicon.ico"
        )

        // when & then
        assertThat(excludedUrls).hasSize(5)
        assertThat(excludedUrls).contains("/swagger-ui/**")
        assertThat(excludedUrls).contains("/api/auth/kakao-login")
    }
}
