package org.depromeet.team3.place.client

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchRequest
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

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


    // TODO: Mockito가 Spring RestClient의 제네릭 타입을 가진 body() 메서드를 처리하지 못하여 테스트 제거
    // @Test
    fun `텍스트 검색 성공`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        val maxResults = 5

        val mockResponse = PlacesTextSearchResponse(
            places = listOf(
                PlacesTextSearchResponse.Place(
                    id = "place_1",
                    displayName = PlacesTextSearchResponse.Place.DisplayName(
                        text = "맛집 1",
                        languageCode = "ko"
                    ),
                    formattedAddress = "서울시 강남구",
                    rating = 4.5,
                    userRatingCount = 100,
                    currentOpeningHours = PlacesTextSearchResponse.Place.OpeningHours(
                        openNow = true
                    )
                )
            )
        )

        // Mock 설정 - body() 호출 이후 부분만 모킹
        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>(lenient = true)
        val requestBodySpec = mock<RestClient.RequestBodySpec>(lenient = true)
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>(lenient = true)
        val responseSpec = mock<RestClient.ResponseSpec>(lenient = true)

        whenever(restClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(any<String>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.header(any<String>(), any<String>())).thenReturn(requestBodySpec)
        // body() 호출 이후 부분만 모킹 - 제네릭 타입 문제 해결을 위해 명시적 타입 지정
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any<PlacesTextSearchRequest>())
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any())
        // body() 호출 이후 부분 모킹 (retrieve()부터)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlacesTextSearchResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.textSearch(query, maxResults)

        // then
        assertThat(result).isNotNull
        assertThat(result.places).isNotNull
        assertThat(result.places!!).hasSize(1)
        assertThat(result.places!![0].displayName.text).isEqualTo("맛집 1")
    }

    // TODO: Mockito가 Spring RestClient의 제네릭 타입을 가진 body() 메서드를 처리하지 못하여 테스트 제거
    // @Test
    fun `텍스트 검색 실패 - 예외 발생 시 PlaceSearchException 던짐`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        
        // Mock 설정 - body() 호출 이후 부분만 모킹
        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>(lenient = true)
        val requestBodySpec = mock<RestClient.RequestBodySpec>(lenient = true)
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>(lenient = true)
        
        whenever(restClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(any<String>())).thenReturn(requestBodySpec)
        // header()가 여러 번 호출되므로 항상 requestBodySpec을 반환하도록 설정
        whenever(requestBodySpec.header(any<String>(), any<String>())).thenReturn(requestBodySpec)
        // body() 호출 이후 부분만 모킹 - 제네릭 타입 문제 해결을 위해 doReturn 사용
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any())
        // body() 호출 이후 부분 모킹 - retrieve()에서 예외 발생
        whenever(requestHeadersSpec.retrieve()).thenThrow(RestClientException("API Error"))

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<org.depromeet.team3.place.exception.PlaceSearchException> {
            runBlocking {
                googlePlacesClient.textSearch(query, 5)
            }
        }
        
        // 재시도 후 최종 실패 시 PLACE_API_ERROR (P002) 반환
        assertThat(exception.errorCode.code).isEqualTo("P002")
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
        assertThat(result.id).isEqualTo(placeId)
        assertThat(result.displayName).isNotNull
        assertThat(result.displayName!!.text).isEqualTo("맛집 1")
        assertThat(result.reviews).isNotNull
        assertThat(result.reviews!!).hasSize(1)
        assertThat(result.reviews!![0].rating).isEqualTo(5.0)
    }

    @Test
    fun `Place Details 조회 실패 - 예외 발생 시 PlaceSearchException 던짐`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        
        // Mock 설정 - 전체 체인 구성
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        // retrieve()에서 RestClientException을 던지도록 설정 (재시도 로직 테스트)
        whenever(requestHeadersSpec.retrieve()).thenThrow(RestClientException("API Error"))

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<org.depromeet.team3.place.exception.PlaceSearchException> {
            runBlocking {
                googlePlacesClient.getPlaceDetails(placeId)
            }
        }
        
        // 재시도 후 최종 실패 시 PLACE_API_ERROR (P002) 반환
        assertThat(exception.errorCode.code).isEqualTo("P002")
    }

    @Test
    fun `Place Details 조회 실패 - 장소를 찾을 수 없음`(): Unit = runBlocking {
        // given
        val placeId = "invalid_place_id"
        
        // Mock 설정 - 전체 체인 구성
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java)).thenThrow(
            org.springframework.web.client.HttpClientErrorException.NotFound.create(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Not Found",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null
            )
        )

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<org.depromeet.team3.place.exception.PlaceSearchException> {
            runBlocking {
                googlePlacesClient.getPlaceDetails(placeId)
            }
        }
        
        assertThat(exception.errorCode.code).isEqualTo("P004")
        assertThat(exception.message).contains("장소를 찾을 수 없습니다")
    }

    /**
     *  추후 수정 - Mockito가 Spring RestClient의 제네릭 타입을 가진 body() 메서드를 처리하지 못하여 테스트 제거
     */
    // @Test
    fun `텍스트 검색 - 여러 결과 반환`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"
        val maxResults = 3

        val mockResponse = PlacesTextSearchResponse(
            places = (1..3).map { index ->
                PlacesTextSearchResponse.Place(
                    id = "place_$index",
                    displayName = PlacesTextSearchResponse.Place.DisplayName(
                        text = "맛집 $index",
                        languageCode = "ko"
                    ),
                    formattedAddress = "서울시 강남구 $index",
                    rating = 4.5,
                    userRatingCount = 100,
                    currentOpeningHours = PlacesTextSearchResponse.Place.OpeningHours(
                        openNow = true
                    )
                )
            }
        )

        // Mock 설정 - body() 호출 이후 부분만 모킹
        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>(lenient = true)
        val requestBodySpec = mock<RestClient.RequestBodySpec>(lenient = true)
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>(lenient = true)
        val responseSpec = mock<RestClient.ResponseSpec>(lenient = true)

        whenever(restClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(any<String>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.header(any<String>(), any<String>())).thenReturn(requestBodySpec)
        // body() 호출 이후 부분만 모킹 - 제네릭 타입 문제 해결을 위해 명시적 타입 지정
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any<PlacesTextSearchRequest>())
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any())
        // body() 호출 이후 부분 모킹 (retrieve()부터)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlacesTextSearchResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.textSearch(query, maxResults)

        // then
        assertThat(result).isNotNull
        assertThat(result.places).isNotNull
        assertThat(result.places!!).hasSize(3)
        assertThat(result.places!!.map { it.displayName.text }).containsExactly("맛집 1", "맛집 2", "맛집 3")
    }

    /**
     *  추후 수정 - Mockito가 Spring RestClient의 제네릭 타입을 가진 body() 메서드를 처리하지 못하여 테스트 제거
     */
    // @Test
    fun `텍스트 검색 - places가 null이면 응답 그대로 반환`(): Unit = runBlocking {
        // given
        val query = "강남역 맛집"

        val mockResponse = PlacesTextSearchResponse(places = null)

        // Mock 설정 - body() 호출 이후 부분만 모킹
        val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>(lenient = true)
        val requestBodySpec = mock<RestClient.RequestBodySpec>(lenient = true)
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>(lenient = true)
        val responseSpec = mock<RestClient.ResponseSpec>(lenient = true)

        whenever(restClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(any<String>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.header(any<String>(), any<String>())).thenReturn(requestBodySpec)
        // body() 호출 이후 부분만 모킹 - 제네릭 타입 문제 해결을 위해 명시적 타입 지정
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any<PlacesTextSearchRequest>())
        doReturn(requestHeadersSpec).whenever(requestBodySpec).body(any())
        // body() 호출 이후 부분 모킹 (retrieve()부터)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlacesTextSearchResponse::class.java)).thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.textSearch(query, 5)

        // then
        assertThat(result).isNotNull
        assertThat(result.places).isNull()
    }

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
        assertThat(result.reviews).isNull()
        assertThat(result.photos).isNull()
    }
}
