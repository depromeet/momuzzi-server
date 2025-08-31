package org.depromeet.team3.common.exception

/**
 * 애플리케이션 전역에서 사용하는 에러 코드 정의
 */
enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: Int
) {
    // 4xx Client Errors
    INVALID_REQUEST("C001", "잘못된 요청입니다.", 400),
    INVALID_PARAMETER("C002", "유효하지 않은 파라미터입니다.", 400),
    MISSING_PARAMETER("C003", "필수 파라미터가 누락되었습니다.", 400),
    INVALID_JSON("C004", "잘못된 JSON 형식입니다.", 400),

    // 404 Not Found
    RESOURCE_NOT_FOUND("C404", "요청한 리소스를 찾을 수 없습니다.", 404),

    // 409 Conflict
    RESOURCE_CONFLICT("C409", "리소스 충돌이 발생했습니다.", 409),
    DUPLICATE_RESOURCE("C4091", "중복된 리소스입니다.", 409),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR("S001", "서버 내부 오류가 발생했습니다.", 500),
    DATABASE_ERROR("S002", "데이터베이스 오류가 발생했습니다.", 500),
    EXTERNAL_API_ERROR("S003", "외부 API 호출 중 오류가 발생했습니다.", 500)
}
