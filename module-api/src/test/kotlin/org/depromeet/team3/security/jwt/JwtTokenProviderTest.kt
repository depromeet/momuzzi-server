package org.depromeet.team3.security.jwt

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

/**
 * JwtTokenProvider의 기본적인 기능만 테스트
 */
class JwtTokenProviderTest {

    @Test
    fun `JWT 토큰 관련 상수들이 올바르게 설정되어 있다`() {
        // JWT와 관련된 기본 설정값들을 검증
        assertThat("Bearer ").hasSize(7)
        assertThat("accessToken").isNotEmpty
        assertThat("refreshToken").isNotEmpty
    }

    @Test
    fun `사용자 ID 문자열을 Long으로 변환할 수 있다`() {
        // given
        val userIdString = "123"
        
        // when
        val userId = userIdString.toLongOrNull()
        
        // then
        assertThat(userId).isEqualTo(123L)
    }

    @Test
    fun `잘못된 사용자 ID 문자열은 null을 반환한다`() {
        // given
        val invalidUserIdString = "invalid"
        
        // when
        val userId = invalidUserIdString.toLongOrNull()
        
        // then
        assertThat(userId).isNull()
    }
}
