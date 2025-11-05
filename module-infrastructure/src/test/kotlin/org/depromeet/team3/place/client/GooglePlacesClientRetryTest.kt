package org.depromeet.team3.place.client

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException


/**
 * GooglePlacesClient의 지수 백오프 재시도 로직 통합 테스트
 * 
 * 핵심 시나리오만 테스트:
 * 1. 재시도 성공 (500 에러 후 성공)
 * 2. 재시도 실패 (최대 재시도 횟수 초과)
 * 3. 재시도하지 않음 (401, 404)
 * 4. 네트워크 오류 재시도
 */
class GooglePlacesClientRetryTest {

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

    @Test
    fun `재시도 성공 - 500 에러 후 재시도하여 성공`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        val mockResponse = createPlaceDetailsResponse(placeId, "맛집 1")
        
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        // 첫 번째 호출: 500 에러, 두 번째 호출: 성공
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java))
            .thenThrow(createHttpClientErrorException(500))
            .thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(placeId)
        assertThat(result.displayName?.text).isEqualTo("맛집 1")
        
        // 재시도 확인: 총 2번 호출 (1번 실패 + 1번 성공)
        verify(responseSpec, times(2)).body(PlaceDetailsResponse::class.java)
    }

    @Test
    fun `재시도 실패 - 최대 재시도 횟수 초과`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        // 모든 호출에서 500 에러 발생
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java))
            .thenThrow(createHttpClientErrorException(500))

        // when & then
        org.junit.jupiter.api.assertThrows<PlaceSearchException> {
            runBlocking {
                googlePlacesClient.getPlaceDetails(placeId)
            }
        }
        
        // 최대 3번 시도 (초기 1번 + 재시도 2번)
        verify(responseSpec, times(3)).body(PlaceDetailsResponse::class.java)
    }

    @Test
    fun `재시도하지 않음 - 401 인증 오류는 즉시 실패`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java))
            .thenThrow(createHttpClientErrorException(401))

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<PlaceSearchException> {
            runBlocking {
                googlePlacesClient.getPlaceDetails(placeId)
            }
        }
        
        assertThat(exception.errorCode.code).isEqualTo(ErrorCode.PLACE_API_KEY_INVALID.code)
        
        // 재시도하지 않음: 1번만 호출
        verify(responseSpec, times(1)).body(PlaceDetailsResponse::class.java)
    }

    @Test
    fun `재시도하지 않음 - 404 Not Found는 즉시 실패`(): Unit = runBlocking {
        // given
        val placeId = "invalid_place_id"
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java))
            .thenThrow(createHttpClientErrorException(404))

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<PlaceSearchException> {
            runBlocking {
                googlePlacesClient.getPlaceDetails(placeId)
            }
        }
        
        assertThat(exception.errorCode.code).isEqualTo(ErrorCode.PLACE_DETAILS_NOT_FOUND.code)
        
        // 재시도하지 않음: 1번만 호출
        verify(responseSpec, times(1)).body(PlaceDetailsResponse::class.java)
    }

    @Test
    fun `네트워크 오류 재시도 - RestClientException 재시도 후 성공`(): Unit = runBlocking {
        // given
        val placeId = "place_1"
        val mockResponse = createPlaceDetailsResponse(placeId, "맛집 1")
        
        val requestHeadersUriSpec = mock<RestClient.RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mock<RestClient.RequestHeadersSpec<*>>()
        val responseSpec = mock<RestClient.ResponseSpec>()
        
        // 첫 번째: 네트워크 오류, 두 번째: 성공
        whenever(restClient.get()).thenReturn(requestHeadersUriSpec)
        whenever(requestHeadersUriSpec.uri(any<String>(), anyVararg<Any>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.header(any<String>(), any<String>())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.body(PlaceDetailsResponse::class.java))
            .thenThrow(RestClientException("Network connection failed"))
            .thenReturn(mockResponse)

        // when
        val result = googlePlacesClient.getPlaceDetails(placeId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(placeId)
        
        // 재시도 확인: 총 2번 호출
        verify(responseSpec, times(2)).body(PlaceDetailsResponse::class.java)
    }

    // Helper functions
    private fun createHttpClientErrorException(statusCode: Int): HttpClientErrorException {
        return when (statusCode) {
            401 -> HttpClientErrorException.Unauthorized.create(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null
            )
            404 -> HttpClientErrorException.NotFound.create(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Not Found",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null
            )
            500 -> HttpClientErrorException.create(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null
            )
            else -> HttpClientErrorException.create(
                org.springframework.http.HttpStatus.valueOf(statusCode),
                "Error",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null
            )
        }
    }

    private fun createPlaceDetailsResponse(placeId: String, name: String): PlaceDetailsResponse {
        return PlaceDetailsResponse(
            id = placeId,
            displayName = PlaceDetailsResponse.DisplayName(
                text = name,
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
    }
}
