package org.depromeet.team3.place.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.depromeet.team3.common.ContextConstants
import org.depromeet.team3.common.annotation.UserId
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.place.application.SearchPlacesService
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory

@Tag(name = "맛집 데이터", description = "구글 플레이스 맛집 검색 API")
@RestController
@RequestMapping("${ContextConstants.API_VERSION_V1}/places")
class PlacesSearchController(
    private val searchPlacesService: SearchPlacesService
) {
    private val logger = LoggerFactory.getLogger(PlacesSearchController::class.java)

    @Operation(
        summary = "맛집 데이터 검색",
        description = "키워드 입력으로 맛집 데이터를 반환 받습니다. meetingId를 포함하면 좋아요 정보가 함께 반환되며 좋아요순으로 정렬됩니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검색 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    @GetMapping
    suspend fun textSearch(
        @Parameter(description = "검색 키워드", example = "강남역 야장 맛집", required = true)
        @RequestParam query: String,
        @Parameter(description = "모임 ID (선택사항, 좋아요 정보 포함시)", example = "1", required = false)
        @RequestParam(required = false) meetingId: Long?,
        @UserId userId: Long?
    ): DpmApiResponse<PlacesSearchResponse> {
        val request = PlacesSearchRequest(
            query = query,
            meetingId = meetingId,
            userId = userId
        )
        val response = searchPlacesService.textSearch(request)
        return DpmApiResponse.ok(response)
    }
}