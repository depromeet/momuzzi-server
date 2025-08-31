package org.depromeet.team3.config

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.exception.DpmException
import org.depromeet.team3.common.response.DpmApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 *  일관된 에러 응답을 보장하기 위한 전역 예외 처리기
 */
@RestControllerAdvice
class ControllerAdvice {

    /**
     * 커스텀 Exception 하위 클래스
     */
    @ExceptionHandler(DpmException::class)
    fun handleDpmException(e: DpmException): ResponseEntity<DpmApiResponse<Unit>> {
        return ResponseEntity.status(e.errorCode.httpStatus)
            .body(DpmApiResponse.error(e))
    }

    /**
     * 위 Exception 이 놓친 모든 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<DpmApiResponse<Unit>> {
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(DpmApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR))
    }
}
