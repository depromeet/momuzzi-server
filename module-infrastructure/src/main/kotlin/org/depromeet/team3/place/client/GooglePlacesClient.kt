package org.depromeet.team3.place.client

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.NearbySearchRequest
import org.depromeet.team3.place.model.NearbySearchResponse
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchRequest
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import kotlin.random.Random

@Component
class GooglePlacesClient(
    private val googlePlacesRestClient: RestClient,
    private val googlePlacesApiProperties: GooglePlacesApiProperties,
) {

    private val logger = KotlinLogging.logger { GooglePlacesClient::class.java.name }
    
    // API 호출 타임아웃 설정 (5초)
    private val apiTimeoutMillis = 5_000L
    
    // 재시도 설정
    private val maxRetries = 3  // 최대 3번 시도 (초기 1번 + 재시도 2번)
    private val initialDelayMillis = 100L // 초기 지연 시간 (100ms)
    private val maxDelayMillis = 2000L // 최대 지연 시간 (2초)
    private val jitterMaxMillis = 100L // 지터 최대값 (0~100ms)

    /**
     * 지수 백오프 재시도 로직
     * 일시적 오류(429, 500-504, 네트워크 오류)에 대해서만 재시도
     */
    private suspend fun <T> retryWithExponentialBackoff(
        operation: String,
        operationDetail: String = "",
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var delayMillis = initialDelayMillis

        // 최대 3번 시도 (초기 1번 + 재시도 2번)
        for (attempt in 0 until maxRetries) {
            try {
                return block()
            } catch (e: HttpClientErrorException) {
                val statusCode = e.statusCode.value()
                // 재시도하지 않을 오류 (401, 404 등)
                if (statusCode in listOf(401, 404)) {
                    throw e
                }
                // 재시도 가능한 오류 (429, 500-504)
                if (statusCode == 429 || statusCode in 500..504) {
                    lastException = e
                    if (attempt < maxRetries - 1) {
                        val jitter = Random.nextLong(0, jitterMaxMillis)
                        val totalDelay = delayMillis + jitter
                        logger.warn(e) { 
                            "$operation 재시도 (${attempt + 1}/${maxRetries - 1}) - 상태코드: $statusCode, $operationDetail, ${totalDelay}ms 후 재시도 (지터: ${jitter}ms)" 
                        }
                        delay(totalDelay)
                        delayMillis = minOf(delayMillis * 2, maxDelayMillis) // 지수 백오프 (다음 재시도를 위해)
                    }
                } else {
                    throw e
                }
            } catch (e: RestClientException) {
                // 네트워크 오류는 재시도
                lastException = e
                if (attempt < maxRetries - 1) {
                    val jitter = Random.nextLong(0, jitterMaxMillis)
                    val totalDelay = delayMillis + jitter
                    logger.warn(e) { 
                        "$operation 재시도 (${attempt + 1}/${maxRetries - 1}) - 네트워크 오류: ${e.message}, $operationDetail, ${totalDelay}ms 후 재시도 (지터: ${jitter}ms)" 
                    }
                    delay(totalDelay)
                    delayMillis = minOf(delayMillis * 2, maxDelayMillis) // 지수 백오프 (다음 재시도를 위해)
                }
            } catch (e: TimeoutCancellationException) {
                // 타임아웃은 재시도하지 않음 (이미 타임아웃으로 처리됨)
                throw e
            } catch (e: PlaceSearchException) {
                // PlaceSearchException은 재시도하지 않음
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val jitter = Random.nextLong(0, jitterMaxMillis)
                    val totalDelay = delayMillis + jitter
                    logger.warn(e) { 
                        "$operation 재시도 (${attempt + 1}/${maxRetries - 1}) - 예외: ${e.javaClass.simpleName}, $operationDetail, ${totalDelay}ms 후 재시도 (지터: ${jitter}ms)" 
                    }
                    delay(totalDelay)
                    delayMillis = minOf(delayMillis * 2, maxDelayMillis) // 지수 백오프 (다음 재시도를 위해)
                }
            }
        }

        // 모든 재시도 실패
        logger.error(lastException) { "$operation 최종 실패 (${maxRetries - 1}회 재시도 후), $operationDetail" }
        // 최종 실패 시에도 일관된 예외 타입을 위해 HttpClientErrorException을 PlaceSearchException으로 변환
        when (val exception = lastException) {
            is HttpClientErrorException -> {
                throw PlaceSearchException(
                    ErrorCode.PLACE_API_ERROR,
                    detail = mapOf("statusCode" to exception.statusCode.value(), "detail" to operationDetail)
                )
            }
            is RestClientException -> {
                throw PlaceSearchException(
                    ErrorCode.PLACE_API_ERROR,
                    detail = mapOf("error" to exception.message, "detail" to operationDetail)
                )
            }
            else -> throw exception ?: PlaceSearchException(
                ErrorCode.PLACE_API_ERROR,
                detail = mapOf("detail" to "$operation 실패")
            )
        }
    }

    /**
     * 텍스트 검색
     */
    suspend fun textSearch(
        query: String, 
        maxResults: Int = 10,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Double = 3000.0
    ): PlacesTextSearchResponse = withContext(Dispatchers.IO) {
        retryWithExponentialBackoff(
            operation = "텍스트 검색",
            operationDetail = "query=$query"
        ) {
            try {
                withTimeout(apiTimeoutMillis) {
                    val locationBias = if (latitude != null && longitude != null) {
                        PlacesTextSearchRequest.LocationBias(
                            circle = PlacesTextSearchRequest.LocationBias.Circle(
                                center = PlacesTextSearchRequest.LocationBias.Circle.Center(
                                    latitude = latitude,
                                    longitude = longitude
                                ),
                                radius = radius
                            )
                        )
                    } else null
                    
                    val request = PlacesTextSearchRequest(
                        textQuery = query,
                        languageCode = "ko",
                        maxResultCount = maxResults,
                        locationBias = locationBias
                    )

                    val response = googlePlacesRestClient.post()
                        .uri("/v1/places:searchText")
                        .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                        .header("X-Goog-FieldMask", buildTextSearchFieldMask())
                        .body(request)
                        .retrieve()
                        .body(PlacesTextSearchResponse::class.java)
                
                    response ?: throw PlaceSearchException(
                        errorCode = ErrorCode.PLACE_API_RESPONSE_NULL,
                        detail = mapOf("query" to query)
                    )
                    response
                }
            } catch (e: TimeoutCancellationException) {
                logger.error(e) { "Google Places API 텍스트 검색 타임아웃: query=$query" }
                throw PlaceSearchException(
                    ErrorCode.PLACE_API_ERROR,
                    detail = mapOf("query" to query, "error" to "요청 타임아웃 (${apiTimeoutMillis}ms 초과)")
                )
            } catch (e: HttpClientErrorException) {
                when (e.statusCode.value()) {
                    401 -> {
                        logger.error(e) { "Google Places API 인증 실패: API 키가 유효하지 않습니다" }
                        throw PlaceSearchException(
                            ErrorCode.PLACE_API_KEY_INVALID
                        )
                    }
                    429 -> {
                        // 재시도 로직에서 처리됨
                        throw e
                    }
                    else -> {
                        // 500-504는 재시도, 나머지는 즉시 실패
                        throw e
                    }
                }
            } catch (e: RestClientException) {
                throw e
            } catch (e: PlaceSearchException) {
                throw e
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Place Details 조회 (New API)
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse = withContext(Dispatchers.IO) {
        retryWithExponentialBackoff(
            operation = "상세 정보 조회",
            operationDetail = "placeId=$placeId"
        ) {
            try {
                withTimeout(apiTimeoutMillis) {
                    val fieldMask = buildPlaceDetailsFieldMask()

                    val response = googlePlacesRestClient.get()
                        .uri("/v1/places/{placeId}?languageCode=ko", placeId)
                        .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                        .header("X-Goog-FieldMask", fieldMask)
                        .retrieve()
                        .body(PlaceDetailsResponse::class.java)
                    
                    response ?: throw PlaceSearchException(
                        errorCode = ErrorCode.PLACE_API_RESPONSE_NULL,
                        detail = mapOf("placeId" to placeId)
                    )
                    response
                }
            } catch (e: TimeoutCancellationException) {
                logger.error(e) { "Google Places API 상세 정보 조회 타임아웃: placeId=$placeId" }
                throw PlaceSearchException(
                    ErrorCode.PLACE_DETAILS_FETCH_FAILED,
                    detail = mapOf("placeId" to placeId, "error" to "요청 타임아웃 (${apiTimeoutMillis}ms 초과)")
                )
            } catch (e: HttpClientErrorException) {
                when (e.statusCode.value()) {
                    401 -> {
                        logger.error(e) { "Google Places API 인증 실패: API 키가 유효하지 않습니다" }
                        throw PlaceSearchException(
                            ErrorCode.PLACE_API_KEY_INVALID
                        )
                    }
                    404 -> {
                        logger.error(e) { "장소를 찾을 수 없음: placeId=$placeId" }
                        throw PlaceSearchException(
                            ErrorCode.PLACE_DETAILS_NOT_FOUND,
                            detail = mapOf("placeId" to placeId)
                        )
                    }
                    else -> {
                        // 재시도 로직에서 처리됨
                        throw e
                    }
                }
            } catch (e: RestClientException) {
                // 재시도 로직에서 처리됨
                throw e
            } catch (e: PlaceSearchException) {
                throw e
            } catch (e: Exception) {
                // 재시도 로직에서 처리됨
                throw e
            }
        }
    }
    
    /**
     * Nearby Search - 주변 지하철역 검색 (New API)
     */
    suspend fun searchNearby(latitude: Double, longitude: Double, radius: Double = 1000.0): NearbySearchResponse = withContext(Dispatchers.IO) {
        retryWithExponentialBackoff(
            operation = "주변 검색",
            operationDetail = "lat=$latitude, lng=$longitude"
        ) {
            try {
                withTimeout(apiTimeoutMillis) {
                    val request = NearbySearchRequest(
                        includedTypes = listOf("subway_station", "transit_station", "train_station"),
                        maxResultCount = 1,
                        locationRestriction = NearbySearchRequest.LocationRestriction(
                            circle = NearbySearchRequest.LocationRestriction.Circle(
                                center = NearbySearchRequest.LocationRestriction.Circle.Center(
                                    latitude = latitude,
                                    longitude = longitude
                                ),
                                radius = radius
                            )
                        ),
                        rankPreference = "DISTANCE"
                    )
                    
                    val response = googlePlacesRestClient.post()
                        .uri("/v1/places:searchNearby")
                        .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                        .header("X-Goog-FieldMask", "places.id,places.displayName,places.location")
                        .header("Accept-Language", "ko")
                        .body(request)
                        .retrieve()
                        .body(NearbySearchResponse::class.java)
                
                    response ?: throw PlaceSearchException(
                        errorCode = ErrorCode.PLACE_API_RESPONSE_NULL,
                        detail = mapOf("latitude" to latitude, "longitude" to longitude)
                    )
                }
            } catch (e: TimeoutCancellationException) {
                logger.error(e) { "Google Places API 주변 검색 타임아웃: lat=$latitude, lng=$longitude" }
                throw PlaceSearchException(
                    ErrorCode.PLACE_NEARBY_SEARCH_FAILED,
                    detail = mapOf("latitude" to latitude, "longitude" to longitude, "error" to "요청 타임아웃 (${apiTimeoutMillis}ms 초과)")
                )
            } catch (e: HttpClientErrorException) {
                when (e.statusCode.value()) {
                    401 -> {
                        logger.error(e) { "Google Places API 인증 실패: API 키가 유효하지 않습니다" }
                        throw PlaceSearchException(
                            ErrorCode.PLACE_API_KEY_INVALID
                        )
                    }
                    else -> {
                        // 재시도 로직에서 처리됨
                        throw e
                    }
                }
            } catch (e: RestClientException) {
                // 재시도 로직에서 처리됨
                throw e
            } catch (e: PlaceSearchException) {
                throw e
            } catch (e: Exception) {
                // 재시도 로직에서 처리됨
                throw e
            }
        }
    }
    
    /**
     * Text Search용 Field Mask
     */
    private fun buildTextSearchFieldMask(): String {
        return listOf(
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.rating",
            "places.userRatingCount",
            "places.currentOpeningHours",
            "places.types"
        ).joinToString(",")
    }
    
    /**
     * Place Details용 Field Mask
     */
    private fun buildPlaceDetailsFieldMask(): String {
        return listOf(
            "id",
            "displayName",
            "formattedAddress",
            "rating",
            "userRatingCount",
            "regularOpeningHours",
            "reviews",
            "photos",
            "priceRange",
            "addressDescriptor",
            "location"
        ).joinToString(",")
    }
}
