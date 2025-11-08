package org.depromeet.team3.place.application.plan
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.place.application.model.PlaceSearchPlan
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.exception.PlaceSearchException
import org.springframework.stereotype.Service

/**
 *  사용자가 입력한 검색어(query) 또는 모임(meetingId)에 따라 검색 전략 결정
 *  내부적으로 CreateSurveyKeywordService 호출하여 설문 기반 키워드 생성
 */
@Service
class CreatePlaceSearchPlanService(
    private val createSurveyKeywordService: CreateSurveyKeywordService
) {
    suspend fun resolve(request: PlacesSearchRequest): PlaceSearchPlan {
        // 설문 결과 기반이 아닌 경우 -> PlaceSearchPlan.Manual 생성
        val normalizedManualQuery = request.query
            ?.let { CreateSurveyKeywordService.normalizeKeyword(it) }
            ?.takeIf { it.isNotBlank() }

        // 설문 결과 기반 -> createSurveyKeywordService 호출
        val stationCoordinates = request.meetingId?.let { createSurveyKeywordService.getStationCoordinates(it) }

        if (normalizedManualQuery != null) {
            return PlaceSearchPlan.Manual(
                keyword = normalizedManualQuery,
                stationCoordinates = stationCoordinates
            )
        }

        val meetingId = request.meetingId ?: throw PlaceSearchException(
            errorCode = ErrorCode.MISSING_PARAMETER,
            detail = mapOf("parameter" to "query|meetingId")
        )

        // 설문 키워드 생성 (PlaceSearchPlan.Automatic)
        val keywordPlan = createSurveyKeywordService.generateKeywordPlan(meetingId)
        if (keywordPlan.keywords.isEmpty()) {
            throw PlaceSearchException(
                errorCode = ErrorCode.SURVEY_RESULT_NOT_FOUND,
                detail = mapOf("meetingId" to meetingId)
            )
        }

        return PlaceSearchPlan.Automatic(
            keywords = keywordPlan.keywords,
            stationCoordinates = keywordPlan.stationCoordinates
        )
    }
}