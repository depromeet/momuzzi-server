package org.depromeet.team3.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.depromeet.team3.security.jwt.JwtProperties
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.depromeet.team3.security.util.CookieUtil
import org.depromeet.team3.security.jwt.AppProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Security 관련 테스트용 설정 클래스
 * @WebMvcTest에서 SecurityConfig를 사용할 때 필요한 의존성들을 Mock으로 제공
 */
@TestConfiguration
class SecurityTestConfig {

    @Bean
    @Primary
    fun testJwtProperties(): JwtProperties {
        return JwtProperties(
            secret = "test-secret-key-for-jwt-token-generation-minimum-256-bits-long",
            accessTokenValidity = 3600000L, // 1시간
            refreshTokenValidity = 604800000L // 1주일
        )
    }

    @Bean
    @Primary
    fun testAppProperties(): AppProperties {
        return AppProperties(
            env = "test"
        )
    }

    @Bean
    @Primary
    fun testCookieUtil(appProperties: AppProperties): CookieUtil {
        return CookieUtil(appProperties)
    }

    @Bean
    @Primary
    fun testJwtTokenProvider(cookieUtil: CookieUtil, jwtProperties: JwtProperties): JwtTokenProvider {
        return JwtTokenProvider(cookieUtil, jwtProperties)
    }

    @Bean
    @Primary
    fun testPasswordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Primary
    fun testObjectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }
}
