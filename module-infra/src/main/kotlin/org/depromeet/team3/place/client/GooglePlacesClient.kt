package org.depromeet.team3.place.client

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

@Component
class GooglePlacesClient(
    private val googlePlacesRestClient: RestClient,
    private val googlePlacesApiProperties: GooglePlacesApiProperties,
) {

    private val logger = KotlinLogging.logger { GooglePlacesClient::class.java.name }

    /**
     * 텍스트 검색
     */
    suspend fun textSearch(query: String, maxResults: Int = 10): PlacesTextSearchResponse = withContext(Dispatchers.IO) {
        try {
            val request = PlacesTextSearchRequest(
                textQuery = query,
                languageCode = "ko",
                maxResultCount = maxResults
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
        } catch (e: HttpClientErrorException) {
            when (e.statusCode.value()) {
                401 -> {
                    logger.error(e) { "Google Places API 인증 실패: API 키가 유효하지 않습니다" }
                    throw PlaceSearchException(
                        ErrorCode.PLACE_API_KEY_INVALID
                    )
                }
                429 -> {
                    logger.error(e) { "Google Places API 할당량 초과" }
                    throw PlaceSearchException(
                        ErrorCode.PLACE_API_QUOTA_EXCEEDED
                    )
                }
                else -> {
                    logger.error(e) { "Google Places API HTTP 에러 - 상태코드: ${e.statusCode}" }
                    throw PlaceSearchException(
                        ErrorCode.PLACE_API_ERROR
                    )
                }
            }
        } catch (e: RestClientException) {
            logger.error(e) { "Google Places API 통신 오류: ${e.message}" }
            throw PlaceSearchException(
                ErrorCode.PLACE_API_ERROR,
                detail = mapOf("error" to e.message)
            )
        } catch (e: Exception) {
            logger.error(e) { "텍스트 검색 실패: query=$query" }
            throw PlaceSearchException(
                ErrorCode.PLACE_SEARCH_FAILED,
                detail = mapOf("query" to query, "error" to e.message)
            )
        }
    }

    /**
     * Place Details 조회 (New API)
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse = withContext(Dispatchers.IO) {
        try {
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
                    logger.error(e) { "Google Places API HTTP 에러 - 상태코드: ${e.statusCode}" }
                    throw PlaceSearchException(
                        ErrorCode.PLACE_API_ERROR
                    )
                }
            }
        } catch (e: RestClientException) {
            logger.error(e) { "Google Places API 통신 오류: ${e.message}" }
            throw PlaceSearchException(
                ErrorCode.PLACE_API_ERROR,
                detail = mapOf("error" to e.message)
            )
        } catch (e: Exception) {
            logger.error(e) { "장소 상세 정보 조회 실패: placeId=$placeId, error=${e.message}" }
            throw PlaceSearchException(
                ErrorCode.PLACE_DETAILS_FETCH_FAILED,
                detail = mapOf("placeId" to placeId, "error" to e.message)
            )
        }
    }
    
    /**
     * Nearby Search - 주변 지하철역 검색 (New API)
     */
    suspend fun searchNearby(latitude: Double, longitude: Double, radius: Double = 1000.0): NearbySearchResponse = withContext(Dispatchers.IO) {
        try {
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
        } catch (e: HttpClientErrorException) {
            when (e.statusCode.value()) {
                401 -> {
                    logger.error(e) { "Google Places API 인증 실패: API 키가 유효하지 않습니다" }
                    throw PlaceSearchException(
                        ErrorCode.PLACE_API_KEY_INVALID
                    )
                }
                else -> {
                    logger.error(e) { "Google Places API HTTP 에러 - 상태코드: ${e.statusCode}" }
                    throw PlaceSearchException(
                        ErrorCode.PLACE_API_ERROR
                    )
                }
            }
        } catch (e: RestClientException) {
            logger.error(e) { "Google Places API 통신 오류: ${e.message}" }
            throw PlaceSearchException(
                ErrorCode.PLACE_API_ERROR,
                detail = mapOf("error" to e.message)
            )
        } catch (e: Exception) {
            logger.error(e) { "주변 역 검색 실패: lat=$latitude, lng=$longitude" }
            throw PlaceSearchException(
                ErrorCode.PLACE_NEARBY_SEARCH_FAILED,
                detail = mapOf("latitude" to latitude, "longitude" to longitude, "error" to e.message)
            )
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
