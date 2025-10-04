package org.depromeet.team3.place.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.depromeet.team3.common.ContextConstants
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.place.application.PlaceSearchService
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "맛집 데이터", description = "구글 플레이스 맛집 검색 API")
@RestController
@RequestMapping("${ContextConstants.API_VERSION_V1}/google-places")
class PlacesSearchController(
    private val placeSearchService: PlaceSearchService
) {

    @Operation(
        summary = "맛집 데이터 검색",
        description = "키워드 입력으로 맛집 데이터를 반환 받습니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검색 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    @GetMapping("/text-search")
    fun textSearch(
        @Parameter(description = "검색 키워드", example = "강남역 한식 맛집", required = true)
        @RequestParam query: String,
        @Parameter(description = "검색 결과 개수", example = "5")
        @RequestParam(defaultValue = "5") maxResults: Int
    ): DpmApiResponse<PlacesSearchResponse> {
        val request = PlacesSearchRequest(query, maxResults)
        val response = placeSearchService.textSearch(request)

        return DpmApiResponse.ok(response)
    }
}
