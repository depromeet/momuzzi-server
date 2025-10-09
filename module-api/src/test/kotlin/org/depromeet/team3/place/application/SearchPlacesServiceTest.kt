package org.depromeet.team3.place.application

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.util.PlaceTestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class SearchPlacesServiceTest {

    @Mock
    private lateinit var placeQuery: PlaceQuery

    private lateinit var googlePlacesApiProperties: GooglePlacesApiProperties
    
    private lateinit var searchPlacesService: SearchPlacesService

    @BeforeEach
    fun setUp() {
        googlePlacesApiProperties = GooglePlacesApiProperties(
            baseUrl = "https://maps.googleapis.com/maps/api/place",
            apiKey = "test-api-key"
        )
        
        searchPlacesService = SearchPlacesService(
            placeQuery = placeQuery,
            googlePlacesApiProperties = googlePlacesApiProperties
        )
    }

    @Test
    fun `맛집 검색 성공 - 첫 번째 요청`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        // Mock place details for each result
        googleResponse.results.take(5).forEach { result ->
            whenever(placeQuery.getPlaceDetails(result.placeId))
                .thenReturn(PlaceTestDataFactory.createGooglePlaceDetailsResponse(result.placeId))
        }

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).hasSize(5)
        assertThat(response.items[0].name).isEqualTo("맛집 1")
        assertThat(response.items[0].topReview).isNotNull
        assertThat(response.items[0].topReview?.rating).isEqualTo(5)
        
        verify(placeQuery).textSearch("강남역 맛집", 10)
        verify(placeQuery, times(5)).getPlaceDetails(any())
    }

    @Test
    fun `맛집 검색 성공 - 두 번째 요청은 다음 페이지 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        googleResponse.results.forEach { result ->
            whenever(placeQuery.getPlaceDetails(result.placeId))
                .thenReturn(PlaceTestDataFactory.createGooglePlaceDetailsResponse(result.placeId))
        }

        // when - 첫 번째 요청
        val firstResponse = searchPlacesService.textSearch(request)
        
        // when - 두 번째 요청 (같은 쿼리)
        val secondResponse = searchPlacesService.textSearch(request)

        // then
        assertThat(firstResponse.items).hasSize(5)
        assertThat(firstResponse.items[0].name).isEqualTo("맛집 1")
        
        assertThat(secondResponse.items).hasSize(5)
        assertThat(secondResponse.items[0].name).isEqualTo("맛집 6")
        
        // 두 응답이 서로 다른 결과를 반환
        assertThat(firstResponse.items.map { it.name })
            .doesNotContainAnyElementsOf(secondResponse.items.map { it.name })
    }

    @Test
    fun `맛집 검색 - 동시성 테스트 Mutex 동작 확인`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        googleResponse.results.forEach { result ->
            whenever(placeQuery.getPlaceDetails(result.placeId))
                .thenReturn(PlaceTestDataFactory.createGooglePlaceDetailsResponse(result.placeId))
        }

        // when - 동시에 2개의 요청 실행
        val results = listOf(
            async { searchPlacesService.textSearch(request) },
            async { searchPlacesService.textSearch(request) }
        ).awaitAll()

        // then - 두 응답은 서로 다른 결과를 반환해야 함 (중복 없음)
        val firstResult = results[0]
        val secondResult = results[1]
        
        assertThat(firstResult.items).hasSize(5)
        assertThat(secondResult.items).hasSize(5)
        
        val firstNames = firstResult.items.map { it.name }.toSet()
        val secondNames = secondResult.items.map { it.name }.toSet()
        
        // 교집합이 없어야 함 (중복된 페이지가 없음)
        assertThat(firstNames.intersect(secondNames)).isEmpty()
    }

    @Test
    fun `맛집 검색 실패 - 최대 호출 횟수 초과`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        googleResponse.results.forEach { result ->
            whenever(placeQuery.getPlaceDetails(result.placeId))
                .thenReturn(PlaceTestDataFactory.createGooglePlaceDetailsResponse(result.placeId))
        }

        // when - 첫 번째, 두 번째 요청
        searchPlacesService.textSearch(request)
        searchPlacesService.textSearch(request)
        
        // when - 세 번째 요청 (최대 호출 횟수 초과)
        val thirdResponse = searchPlacesService.textSearch(request)

        // then - 빈 목록 반환
        assertThat(thirdResponse.items).isEmpty()
    }

    @Test
    fun `맛집 검색 실패 - Google API 오류`() {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        
        runBlocking {
            whenever(placeQuery.textSearch(any(), any()))
                .thenThrow(RuntimeException("API Error"))
        }

        // when & then
        val exception = assertThrows<PlaceSearchException> {
            runBlocking {
                searchPlacesService.textSearch(request)
            }
        }
        
        assertThat(exception.message).isEqualTo("맛집 검색 중 오류가 발생했습니다")
    }

    @Test
    fun `맛집 검색 - ZERO_RESULTS는 빈 목록 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(
            resultCount = 0,
            status = "ZERO_RESULTS"
        )
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 빈 목록 반환
        assertThat(response.items).isEmpty()
    }

    @Test
    fun `맛집 검색 실패 - Google API 에러 상태 코드`() {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(status = "INVALID_REQUEST")
        
        runBlocking {
            whenever(placeQuery.textSearch(any(), any()))
                .thenReturn(googleResponse)
        }

        // when & then
        val exception = assertThrows<PlaceSearchException> {
            runBlocking {
                searchPlacesService.textSearch(request)
            }
        }
        
        assertThat(exception.message).isEqualTo("Google Places API 응답 상태: INVALID_REQUEST")
    }

    @Test
    fun `맛집 검색 - 상세 정보 조회 실패 시 해당 항목만 제외`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        // 첫 번째와 세 번째는 성공, 두 번째는 실패
        googleResponse.results.take(5).forEachIndexed { index, result ->
            if (index == 1) {
                whenever(placeQuery.getPlaceDetails(result.placeId))
                    .thenThrow(RuntimeException("Detail fetch failed"))
            } else {
                whenever(placeQuery.getPlaceDetails(result.placeId))
                    .thenReturn(PlaceTestDataFactory.createGooglePlaceDetailsResponse(result.placeId))
            }
        }

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 실패한 항목만 제외되고 4개 반환
        assertThat(response.items).hasSize(4)
        assertThat(response.items.map { it.name }).doesNotContain("맛집 2")
    }
}
