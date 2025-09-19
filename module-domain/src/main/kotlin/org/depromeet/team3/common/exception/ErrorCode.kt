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
    CATEGORY_NOT_FOUND("C4041", "카테고리를 찾을 수 없습니다.", 404),
    PARENT_CATEGORY_NOT_FOUND("C4042", "부모 카테고리를 찾을 수 없습니다.", 404),

    // 409 Conflict
    RESOURCE_CONFLICT("C409", "리소스 충돌이 발생했습니다.", 409),
    DUPLICATE_RESOURCE("C4091", "중복된 리소스입니다.", 409),
    CATEGORY_HAS_CHILDREN("C4092", "하위 카테고리가 존재하여 삭제할 수 없습니다.", 409),
    INVALID_CATEGORY_LEVEL_CHANGE("C4093", "자식 카테고리가 있는 BRANCH 카테고리를 LEAF로 변경할 수 없습니다.", 409),
    DUPLICATE_CATEGORY_NAME("C4094", "같은 부모 하위에 동일한 이름의 카테고리가 이미 존재합니다.", 409),
    DUPLICATE_CATEGORY_ORDER("C4095", "같은 부모 하위에 동일한 순서의 카테고리가 이미 존재합니다.", 409),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR("S001", "서버 내부 오류가 발생했습니다.", 500),
    DATABASE_ERROR("S002", "데이터베이스 오류가 발생했습니다.", 500),
    EXTERNAL_API_ERROR("S003", "외부 API 호출 중 오류가 발생했습니다.", 500),

    // OAuth 관련 에러
    KAKAO_INVALID_GRANT("O001", "카카오 인증 코드가 유효하지 않습니다.", 401),
    KAKAO_AUTH_FAILED("O002", "카카오 인증에 실패했습니다.", 401),
    KAKAO_JSON_PARSE_ERROR("O003", "카카오 응답 데이터 파싱에 실패했습니다.", 500),
    KAKAO_API_ERROR("O004", "카카오 API 호출 중 오류가 발생했습니다.", 500),
    SOCIAL_LOGIN_INVALID_STATE("O005", "OAuth state 값이 유효하지 않습니다.", 400),
    ALREADY_REGISTERED_WITH_OTHER_LOGIN("O006", "다른 소셜 로그인으로 이미 가입된 이메일입니다.", 409)
}
