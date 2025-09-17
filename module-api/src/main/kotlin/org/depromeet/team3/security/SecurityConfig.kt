package org.depromeet.team3.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.depromeet.team3.security.jwt.JwtAuthenticationFilter
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.depromeet.team3.security.util.CookieUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
    private val cookieUtil: CookieUtil
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/",
                    "/api/auth/**",
                    "/auth/callback/**",  // 카카오 OAuth 콜백 경로 추가
                    "/swagger", "/swagger/", "/swagger-ui/**", "/v3/api-docs/**",
                    "/index.html", "/static/**", "/favicon.ico"
                ).permitAll()
                // it.anyRequest().authenticated()
                it.anyRequest().permitAll()     // 일단 전부 열어놓겠습니다.
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider, objectMapper, cookieUtil),
                UsernamePasswordAuthenticationFilter::class.java
            )
        return http.build()
    }
}