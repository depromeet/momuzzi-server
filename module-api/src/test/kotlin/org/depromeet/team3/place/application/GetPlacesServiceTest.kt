package org.depromeet.team3.place.application

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.place.application.facade.GetPlacesService
import org.depromeet.team3.place.application.plan.CreatePlaceSearchPlanService
import org.depromeet.team3.place.application.execution.ExecutePlaceSearchService
import org.depromeet.team3.place.application.model.PlaceSearchPlan
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class GetPlacesServiceTest {

    @Mock
    private lateinit var createPlaceSearchPlanService: CreatePlaceSearchPlanService

    @Mock
    private lateinit var executePlaceSearchService: ExecutePlaceSearchService
    
    private lateinit var getPlacesService: GetPlacesService

    @BeforeEach
    fun setUp() {
        getPlacesService = GetPlacesService(
            createPlaceSearchPlanService = createPlaceSearchPlanService,
            executePlaceSearchService = executePlaceSearchService
        )
    }

    @Test
    fun `수동 검색 시 계획 수립 후 실행 서비스 호출`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", meetingId = 1L)
        val plan = PlaceSearchPlan.Manual(
            keyword = "강남역 맛집",
            stationCoordinates = null
        )
        val expectedResponse = PlacesSearchResponse(emptyList())

        whenever(createPlaceSearchPlanService.resolve(request))
            .thenReturn(plan)
        whenever(executePlaceSearchService.search(request, plan))
            .thenReturn(expectedResponse)

        // when
        val response = getPlacesService.textSearch(request)

        // then
        assertThat(response).isEqualTo(expectedResponse)
        verify(createPlaceSearchPlanService).resolve(request)
        verify(executePlaceSearchService).search(request, plan)
    }

    @Test
    fun `자동 검색 시 계획 수립 후 실행 서비스 호출`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = null, meetingId = 1L)
        val plan = PlaceSearchPlan.Automatic(
            keywords = emptyList(),
            stationCoordinates = null
        )
        val expectedResponse = PlacesSearchResponse(emptyList())

        whenever(createPlaceSearchPlanService.resolve(request))
            .thenReturn(plan)
        whenever(executePlaceSearchService.search(request, plan))
            .thenReturn(expectedResponse)

        // when
        val response = getPlacesService.textSearch(request)

        // then
        assertThat(response).isEqualTo(expectedResponse)
        verify(createPlaceSearchPlanService).resolve(request)
        verify(executePlaceSearchService).search(request, plan)
    }
}
