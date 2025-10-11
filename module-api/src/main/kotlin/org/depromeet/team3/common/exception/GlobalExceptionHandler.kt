package org.depromeet.team3.common.exception

import org.depromeet.team3.common.response.DpmApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 전역 예외 처리 핸들러
 * - 도메인 예외(DpmException)
 * - Validation 및 요청 파라미터 예외
 * - 잘못된 인자
 * - 예상치 못한 예외
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 도메인 예외 처리
     */
    @ExceptionHandler(DpmException::class)
    fun handleDpmException(e: DpmException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Business exception: [${e.errorCode.name}] ${e.message}", e)
        return ResponseEntity.status(e.errorCode.httpStatus)
            .body(DpmApiResponse.error(e))
    }

    /**
     * Validation 예외 처리 (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Validation failed: ${e.message}", e)
        val errorDetails = mutableMapOf<String, Any?>()
        e.bindingResult.fieldErrors.forEach { fieldError ->
            errorDetails[fieldError.field] = fieldError.defaultMessage
        }
        return ResponseEntity.status(ErrorCode.INVALID_PARAMETER.httpStatus)
            .body(DpmApiResponse.error(ErrorCode.INVALID_PARAMETER, errorDetails))
    }

    /**
     * 필수 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Missing required parameter: ${e.parameterName}", e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(DpmApiResponse.error(ErrorCode.MISSING_PARAMETER))
    }

    /**
     * 잘못된 인자
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<DpmApiResponse<Unit>> {
        logger.warn("Illegal argument: ${e.message}", e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(DpmApiResponse.error(ErrorCode.INVALID_PARAMETER))
    }

    /**
     * 정적 리소스를 찾을 수 없을 때 (로그 출력하지 않음)
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<Unit> {
        // 아무것도 하지 않음 (로그도 남기지 않음)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    /**
     * 예상치 못한 예외
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<DpmApiResponse<Unit>> {
        logger.error("Unexpected exception: ${e.message}", e)
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(DpmApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR))
    }
}