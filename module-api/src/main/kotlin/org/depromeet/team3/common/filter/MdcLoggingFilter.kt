package org.depromeet.team3.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * MDC(Mapped Diagnostic Context)를 활용한 요청 추적 필터
 * 
 * 각 HTTP 요청마다 고유한 request_id를 생성하여 MDC에 저장하고,
 * 요청 처리 시간과 기본 정보를 로깅합니다.
 * 
 * 멀티스레드 환경에서 동일한 요청의 로그를 추적할 수 있도록 합니다.
 */
@Component
class MdcLoggingFilter : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(MdcLoggingFilter::class.java)
    private val pathMatcher = AntPathMatcher()
    
    companion object {
        const val REQUEST_ID = "request_id"
        
        // 필터를 적용하지 않을 URL 패턴 목록
        private val WHITE_LIST = listOf(
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 고유한 요청 ID 생성 (UUID의 앞 8자리만 사용)
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        MDC.put(REQUEST_ID, requestId)
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 다음 필터 체인으로 요청 전달
            filterChain.doFilter(request, response)
        } finally {
            // 요청 처리 완료 후 로깅
            val duration = System.currentTimeMillis() - startTime
            
            logger.info(
                "HTTP Request Completed - status={} method={} uri={} duration={}ms",
                response.status,
                request.method,
                request.requestURI,
                duration
            )
            
            // MDC 정리 (다른 요청에 영향을 주지 않도록)
            MDC.clear()
        }
    }

    /**
     * 화이트리스트에 포함된 URL은 필터를 적용하지 않음
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return WHITE_LIST.any { pattern -> pathMatcher.match(pattern, uri) }
    }
}
