package org.depromeet.team3.survey.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.depromeet.team3.common.ContextConstants
import org.depromeet.team3.common.annotation.UserId
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.survey.application.CreateSurveyService
import org.depromeet.team3.survey.application.GetSurveyListService
import org.depromeet.team3.survey.dto.request.SurveyCreateRequest
import org.depromeet.team3.survey.dto.response.SurveyCreateResponse
import org.depromeet.team3.survey.dto.response.SurveyListResponse
import org.springframework.web.bind.annotation.*

@Tag(name = "설문", description = "설문 관리 API")
@RestController
@RequestMapping("${ContextConstants.API_VERSION_V1}/meetings/{meetingId}/surveys")
class SurveyController(
    private val createSurveyService: CreateSurveyService,
    private val getSurveyListService: GetSurveyListService
) {

    @Operation(
        summary = "모임별 설문 생성",
        description = "특정 모임에 대한 설문을 생성합니다. selectedCategoryList에 선택된 카테고리의 ID를 배열로 전송합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "설문 생성 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ApiResponse(responseCode = "404", description = "모임 또는 참가자를 찾을 수 없음"),
        ApiResponse(responseCode = "409", description = "중복 설문 제출")
    )
    @PostMapping
    fun createSurvey(
        @Parameter(description = "모임 ID", example = "1")
        @PathVariable meetingId: Long,
        @UserId userId: Long,
        @RequestBody @Valid request: SurveyCreateRequest
    ): DpmApiResponse<SurveyCreateResponse> {
        val response = createSurveyService.invoke(meetingId, userId, request)
        return DpmApiResponse.ok(response)
    }

    @Operation(
        summary = "모임별 전체 설문 결과 조회",
        description = "특정 모임의 모든 설문 결과를 조회합니다. 각 설문 항목은 participantId, nickname, selectedCategoryList를 포함합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "설문 결과 조회 성공"),
        ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음")
    )
    @GetMapping
    fun getSurveyList(
        @Parameter(description = "모임 ID", example = "1")
        @PathVariable meetingId: Long,
        @UserId userId: Long
    ): DpmApiResponse<SurveyListResponse> {
        val response = getSurveyListService.invoke(meetingId, userId)
        return DpmApiResponse.ok(response)
    }
}
