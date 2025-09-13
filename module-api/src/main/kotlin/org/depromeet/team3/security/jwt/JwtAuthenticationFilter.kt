package org.depromeet.team3.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.user.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException


@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val UserRepository: UserRepository,
) : OncePerRequestFilter() {
    
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        /**
         *  여기 포함된 URL 들은 필터 통과
         */
        val requestURI = request.requestURI
        if (isExcluded(requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val token = extractToken(request)

            if (token != null && jwtTokenProvider.validateAccessToken(token)) {
                val userId: Long? = jwtTokenProvider.getUserIdFromToken(token)?.toLongOrNull()

                val authorities = listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER"))

                val authentication = JwtAuthenticationToken(userId, authorities)

                SecurityContextHolder.getContext().authentication = authentication
            } else {
                SecurityContextHolder.clearContext()
            }

            // 인증 성공하든, 토큰이 없어도 (인증 제외가 아니므로) 무조건 필터 통과 시도
            filterChain.doFilter(request, response)
        } catch (e: AuthException) {
            logger.warn("[401] JWT 필터 인증 실패", e)
            handleAuthException(response, e)
        } catch (e: Exception) {
            logger.error("[500] JWT 필터 처리 중 예상치 못한 오류 발생", e)
            handleInternalServerError(response)
        }
    }

    private fun isExcluded(requestURI: String): Boolean {
        return EXCLUDED_URLS.any { prefix ->
            requestURI.startsWith(prefix)
        }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        return jwtTokenProvider.extractToken(request)
    }

    /**
     *  인증 실패 시 401
     *  Filter 예외라 Filter 내부에서 처리
     */
    private fun handleAuthException(response: HttpServletResponse, e: AuthException) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"

        val errorResponse = DpmApiResponse.error(e)
        val json = ObjectMapper().writeValueAsString(errorResponse)
        response.writer.write(json)
    }

    private fun handleInternalServerError(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.contentType = "application/json;charset=UTF-8"

        val errorResponse = DpmApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR)
        val json = ObjectMapper().writeValueAsString(errorResponse)
        response.writer.write(json)
    }

    companion object {
        private val EXCLUDED_URLS = listOf(
            "/swagger-ui/**", "/v3/api-docs/**", "/swagger", "/api/auth/kakao-login", "/favicon.ico"
        )
    }
}
