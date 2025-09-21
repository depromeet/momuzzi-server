package org.depromeet.team3.common.exception

import org.depromeet.team3.common.response.DpmApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 전역 예외 처리 핸들러
 * - 도메인 레벨 예외: 각 도메인에서 정의한 Exception들이 DpmException을 상속하여 처리
 * - 프레임워크 레벨 예외: Spring Framework 레벨의 예외들 처리
 * - 시스템 레벨 예외: 예상치 못한 예외들 처리
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 모든 도메인 예외 처리
     */
    @ExceptionHandler(DpmException::class)
    fun handleDpmException(e: DpmException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Business exception occurred: [${e.errorCode.name}] ${e.message}", e)
        val response = DpmApiResponse.error(e)
        return ResponseEntity.status(e.errorCode.httpStatus).body(response)
    }

    /**
     * 필수 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Missing required parameter: ${e.parameterName}", e)
        val response = DpmApiResponse.error(ErrorCode.MISSING_PARAMETER)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * 잘못된 인자 (도메인 예외로 처리되지 않은 경우)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Illegal argument exception: ${e.message}", e)
        val response = DpmApiResponse.error(ErrorCode.INVALID_PARAMETER)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * 예상치 못한 시스템 레벨 예외
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<DpmApiResponse<Unit>> {
        logger.error("Unexpected exception occurred: ${e.message}", e)
        val response = DpmApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}