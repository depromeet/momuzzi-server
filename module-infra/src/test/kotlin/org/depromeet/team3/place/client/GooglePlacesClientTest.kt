package org.depromeet.team3.place.client

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.web.client.RestClient

class GooglePlacesClientTest {

    private lateinit var restClient: RestClient
    private lateinit var googlePlacesApiProperties: GooglePlacesApiProperties
    private lateinit var googlePlacesClient: GooglePlacesClient

    @BeforeEach
    fun setUp() {
        restClient = mock()
        googlePlacesApiProperties = GooglePlacesApiProperties(
            apiKey = "test-api-key",
            baseUrl = "https://places.googleapis.com"
        )
        
        googlePlacesClient = GooglePlacesClient(
            googlePlacesRestClient = restClient,
            googlePlacesApiProperties = googlePlacesApiProperties
        )
    }

    /**
     *  추후 수정
     */
//    @Test
//    fun `텍스트 검색 성공`(): Unit = runBlocking {
//        // given
//        val query = "강남역 맛집"
//        val maxResults = 5
//
//        val mockResponse = PlacesTextSearchResponse(
//            places = listOf(
//                PlacesTextSearchResponse.Place(
//                    id = "place_1",
//                    displayName = PlacesTextSearchResponse.Place.DisplayName(
//                        text = "맛집 1",
//                        languageCode = "ko"
//                    ),
//                    formattedAddress = "서울시 강남구",
//                    rating = 4.5,
//                    userRatingCount = 100,
//                    currentOpeningHours = PlacesTextSearchResponse.Place.OpeningHours(
//                        openNow = true
//                    )
//                )
//            )
//        )
//
//        // Mock 설정
//        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>()
//        val requestBodySpec = mock<RestClient.RequestBodySpec>()
//        val responseSpec = mock<RestClient.ResponseSpec>()
//
//        doReturn(requestBodyUriSpec).`when`(restClient).post()
//        doReturn(requestBodySpec).`when`(requestBodyUriSpec).uri(any<String>())
//        doReturn(requestBodySpec).`when`(requestBodySpec).header(any<String>(), any<String>())
//        doReturn(requestBodySpec).`when`(requestBodySpec).body(any())
//        doReturn(responseSpec).`when`(requestBodySpec).retrieve()
//        doReturn(mockResponse).`when`(responseSpec).body(PlacesTextSearchResponse::class.java)
//
//        // when
//        val result = googlePlacesClient.textSearch(query, maxResults)
//
//        // then
//        assertThat(result).isNotNull
//        assertThat(result?.places).hasSize(1)
//        assertThat(result?.places?.get(0)?.displayName?.text).isEqualTo("맛집 1")
//    }

    @Test
    fun `텍스트 검색 실패 - 예외 발생 시 null 반환`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        
        // Mock 설정
        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>()
        val requestBodySpec = mock<RestClient.RequestBodySpec>()
        
        whenever(restClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(any<String>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.header(any<String>(), any<String>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenThrow(RuntimeException("API Error"))

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
            id = placeId,
            displayName = PlaceDetailsResponse.DisplayName(
                text = "맛집 1",
                languageCode = "ko"
            ),
            formattedAddress = "서울시 강남구",
            rating = 4.5,
            userRatingCount = 100,
            regularOpeningHours = PlaceDetailsResponse.OpeningHours(
                weekdayDescriptions = listOf("월요일: 10:00~22:00")
            ),
            reviews = listOf(
                PlaceDetailsResponse.Review(
                    authorAttribution = PlaceDetailsResponse.Review.AuthorAttribution(
                        displayName = "리뷰어"
                    ),
                    rating = 5.0,
                    relativePublishTimeDescription = "1주 전",
                    text = PlaceDetailsResponse.Review.TextContent(
                        text = "맛있어요",
                        languageCode = "ko"
                    )
                )
            ),
            photos = listOf(
                PlaceDetailsResponse.Photo(
                    name = "places/place_1/photos/photo_1",
                    widthPx = 800,
                    heightPx = 800
                )
            ),
            priceRange = null,
            addressDescriptor = null,
            location = PlaceDetailsResponse.Location(
                latitude = 37.5665,
                longitude = 126.9780
            )
        )
        
        // Mock 설정
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(placeId)
        assertThat(result?.displayName?.text).isEqualTo("맛집 1")
        assertThat(result?.reviews).hasSize(1)
        assertThat(result?.reviews?.get(0)?.rating).isEqualTo(5.0)
    }

    @Test
    fun `Place Details 조회 실패 - 예외 발생 시 null 반환`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        
        // Mock 설정
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenThrow(RuntimeException("API Error"))

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNull()
    }

    /**
     *  추후 수정
     */
//    @Test
//    fun `텍스트 검색 - 여러 결과 반환`(): Unit = runBlocking {
//        // given
//        val query = "강남역 맛집"
//        val maxResults = 3
//
//        val mockResponse = PlacesTextSearchResponse(
//            places = (1..3).map { index ->
//                PlacesTextSearchResponse.Place(
//                    id = "place_$index",
//                    displayName = PlacesTextSearchResponse.Place.DisplayName(
//                        text = "맛집 $index",
//                        languageCode = "ko"
//                    ),
//                    formattedAddress = "서울시 강남구 $index",
//                    rating = 4.5,
//                    userRatingCount = 100,
//                    currentOpeningHours = PlacesTextSearchResponse.Place.OpeningHours(
//                        openNow = true
//                    )
//                )
//            }
//        )
//
//        // Mock 설정
//        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>()
//        val requestBodySpec = mock<RestClient.RequestBodySpec>()
//        val responseSpec = mock<RestClient.ResponseSpec>()
//
//        doReturn(requestBodyUriSpec).`when`(restClient).post()
//        doReturn(requestBodySpec).`when`(requestBodyUriSpec).uri(any<String>())
//        doReturn(requestBodySpec).`when`(requestBodySpec).header(any<String>(), any<String>())
//        doReturn(requestBodySpec).`when`(requestBodySpec).body(any())
//        doReturn(responseSpec).`when`(requestBodySpec).retrieve()
//        doReturn(mockResponse).`when`(responseSpec).body(PlacesTextSearchResponse::class.java)
//
//        // when
//        val result = googlePlacesClient.textSearch(query, maxResults)
//
//        // then
//        assertThat(result).isNotNull
//        assertThat(result?.places).hasSize(3)
//        assertThat(result?.places?.map { it.displayName.text }).containsExactly("맛집 1", "맛집 2", "맛집 3")
//    }

    /**
     *  추후 수정
     */
//    @Test
//    fun `텍스트 검색 - places가 null이면 응답 그대로 반환`(): Unit = runBlocking {
//        // given
//        val query = "강남역 맛집"
//
//        val mockResponse = PlacesTextSearchResponse(places = null)
//
//        // Mock 설정
//        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>()
//        val requestBodySpec = mock<RestClient.RequestBodySpec>()
//        val responseSpec = mock<RestClient.ResponseSpec>()
//
//        doReturn(requestBodyUriSpec).`when`(restClient).post()
//        doReturn(requestBodySpec).`when`(requestBodyUriSpec).uri(any<String>())
//        doReturn(requestBodySpec).`when`(requestBodySpec).header(any<String>(), any<String>())
//        doReturn(requestBodySpec).`when`(requestBodySpec).body(any())
//        doReturn(responseSpec).`when`(requestBodySpec).retrieve()
//        doReturn(mockResponse).`when`(responseSpec).body(PlacesTextSearchResponse::class.java)
//
//        // when
//        val result = googlePlacesClient.textSearch(query, 5)
//
//        // then
//        assertThat(result).isNotNull()
//        assertThat(result?.places).isNull()
//    }

    @Test
    fun `Place Details 조회 - 리뷰가 없는 경우`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        
        val mockResponse = PlaceDetailsResponse(
            id = placeId,
            displayName = PlaceDetailsResponse.DisplayName(
                text = "맛집 1",
                languageCode = "ko"
            ),
            formattedAddress = "서울시 강남구",
            rating = 4.5,
            userRatingCount = 100,
            regularOpeningHours = null,
            reviews = null,
            photos = null,
            priceRange = null,
            addressDescriptor = null,
            location = null
        )
        
        // Mock 설정
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNotNull
        assertThat(result?.reviews).isNull()
        assertThat(result?.photos).isNull()
    }
}
