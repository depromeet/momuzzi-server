package org.depromeet.team3.place.application.facade

import org.depromeet.team3.place.application.plan.CreatePlaceSearchPlanService
import org.depromeet.team3.place.application.execution.ExecutePlaceSearchService
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.springframework.stereotype.Service

/**
 * 검색 요청을 받아 검색 계획 수립과 실행을 오케스트레이션하고
 * PlaceSearchPlanService와 PlaceSearchExecutionService를 순차 호출한다.
 */
@Service
class GetPlacesService(
    private val createPlaceSearchPlanService: CreatePlaceSearchPlanService,
    private val executePlaceSearchService: ExecutePlaceSearchService
) {
    suspend fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse {
        val plan = createPlaceSearchPlanService.resolve(request)
        return executePlaceSearchService.search(request, plan)
    }
}