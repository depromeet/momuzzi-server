package org.depromeet.team3.place.client

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesSearchResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.function.Function
import org.springframework.web.util.UriBuilder

@ExtendWith(MockitoExtension::class)
class GooglePlacesClientTest {

    @Mock
    private lateinit var restClient: RestClient

    @Mock
    private lateinit var requestHeadersUriSpec: RestClient.RequestHeadersUriSpec<*>

    @Mock
    private lateinit var requestHeadersSpec: RestClient.RequestHeadersSpec<*>

    @Mock
    private lateinit var responseSpec: RestClient.ResponseSpec

    private lateinit var googlePlacesApiProperties: GooglePlacesApiProperties
    
    private lateinit var googlePlacesClient: GooglePlacesClient

    @BeforeEach
    fun setUp() {
        googlePlacesApiProperties = GooglePlacesApiProperties(
            apiKey = "test-api-key",
            baseUrl = "https://maps.googleapis.com/maps/api/place"
        )
        
        googlePlacesClient = GooglePlacesClient(
            googlePlacesRestClient = restClient,
            googlePlacesApiProperties = googlePlacesApiProperties
        )
    }

    @Test
    fun `텍스트 검색 성공`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        val maxResults = 5
        
        val mockResponse = PlacesSearchResponse(
            results = listOf(
                PlacesSearchResponse.PlaceResult(
                    placeId = "place_1",
                    name = "맛집 1",
                    formattedAddress = "서울시 강남구",
                    rating = 4.5,
                    userRatingsTotal = 100,
                    openingHours = null,
                    url = null,
                    photos = null
                )
            ),
            status = "OK"
        )
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<Function<UriBuilder, URI>>()))
            .thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlacesSearchResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.textSearch(query, maxResults)

        // then
        assertThat(result).isNotNull
        assertThat(result?.status).isEqualTo("OK")
        assertThat(result?.results).hasSize(1)
        assertThat(result?.results?.get(0)?.name).isEqualTo("맛집 1")
    }

    @Test
    fun `텍스트 검색 실패 - 예외 발생 시 null 반환`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<Function<UriBuilder, URI>>()))
            .thenReturn(requestHeadersSpec)

        // when
        val result = googlePlacesClient.textSearch(query, 5)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `Place Details 조회 성공`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        
        val mockResponse = PlaceDetailsResponse(
            result = PlaceDetailsResponse.PlaceDetail(
                openingHours = PlaceDetailsResponse.PlaceDetail.OpeningHours(
                    weekdayText = listOf("월요일: 10:00~22:00")
                ),
                reviews = listOf(
                    PlaceDetailsResponse.PlaceDetail.Review(
                        authorName = "리뷰어",
                        rating = 5.0,
                        relativeTimeDescription = "1주 전",
                        text = "맛있어요",
                        time = 123456789L
                    )
                ),
                photos = null
            ),
            status = "OK"
        )
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<Function<UriBuilder, URI>>()))
            .thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNotNull
        assertThat(result?.status).isEqualTo("OK")
        assertThat(result?.result).isNotNull
        assertThat(result?.result?.reviews).hasSize(1)
        assertThat(result?.result?.reviews?.get(0)?.rating).isEqualTo(5.0)
    }

    @Test
    fun `Place Details 조회 실패 - 예외 발생 시 null 반환`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<Function<UriBuilder, URI>>()))
            .thenReturn(requestHeadersSpec)

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `텍스트 검색 결과 maxResults 만큼만 반환`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        val maxResults = 3
        
        val mockResponse = PlacesSearchResponse(
            results = (1..10).map { index ->
                PlacesSearchResponse.PlaceResult(
                    placeId = "place_$index",
                    name = "맛집 $index",
                    formattedAddress = "서울시 강남구 $index",
                    rating = 4.5,
                    userRatingsTotal = 100,
                    openingHours = null,
                    url = null,
                    photos = null
                )
            },
            status = "OK"
        )
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<Function<UriBuilder, URI>>()))
            .thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlacesSearchResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.textSearch(query, maxResults)

        // then
        assertThat(result).isNotNull
        assertThat(result?.results).hasSize(3)
        assertThat(result?.results?.map { it.name }).containsExactly("맛집 1", "맛집 2", "맛집 3")
    }
}
