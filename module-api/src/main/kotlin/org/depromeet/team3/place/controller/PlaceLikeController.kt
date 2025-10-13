package org.depromeet.team3.place.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.depromeet.team3.common.ContextConstants
import org.depromeet.team3.common.annotation.UserId
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.place.application.SearchPlaceLikeService
import org.depromeet.team3.place.dto.response.PlaceLikeResponse
import org.springframework.web.bind.annotation.*

@Tag(name = "맛집 좋아요", description = "맛집 좋아요 API")
@RestController
@RequestMapping("${ContextConstants.API_VERSION_V1}/meetings/{meetingId}/places/{placeId}/like")
class PlaceLikeController(
    private val searchPlaceLikeService: SearchPlaceLikeService
) {

    @Operation(
        summary = "맛집 좋아요 토글",
        description = "맛집에 좋아요를 추가하거나 취소합니다. 이미 좋아요가 있으면 취소, 없으면 추가합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청")
    )
    @PostMapping
    suspend fun toggleLike(
        @Parameter(description = "모임 ID", required = true)
        @PathVariable meetingId: Long,
        @Parameter(description = "맛집 Place DB ID", required = true)
        @PathVariable placeId: Long,
        @UserId userId: Long
    ): DpmApiResponse<PlaceLikeResponse> {
        val isLiked = searchPlaceLikeService.toggle(meetingId, userId, placeId)
        
        val response = PlaceLikeResponse(
            isLiked = isLiked,
            message = if (isLiked) "좋아요를 추가했습니다." else "좋아요를 취소했습니다."
        )
        return DpmApiResponse.ok(response)
    }
}