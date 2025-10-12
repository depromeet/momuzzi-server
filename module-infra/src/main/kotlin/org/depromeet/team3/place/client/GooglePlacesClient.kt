package org.depromeet.team3.place.client

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchRequest
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GooglePlacesClient(
    private val googlePlacesRestClient: RestClient,
    private val googlePlacesApiProperties: GooglePlacesApiProperties,
) {

    private val logger = KotlinLogging.logger { GooglePlacesClient::class.java.name }

    /**
     * 텍스트 검색 (New API)
     */
    suspend fun textSearch(query: String, maxResults: Int = 10): PlacesTextSearchResponse? = withContext(Dispatchers.IO) {
        try {
            val request = PlacesTextSearchRequest(
                textQuery = query,
                languageCode = "ko",
                maxResultCount = maxResults
            )
            
            googlePlacesRestClient.post()
                .uri("/v1/places:searchText")
                .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                .header("X-Goog-FieldMask", buildTextSearchFieldMask())
                .body(request)
                .retrieve()
                .body(PlacesTextSearchResponse::class.java)
        } catch (e: Exception) {
            logger.error(e) { "텍스트 검색 실패: query=$query" }
            null
        }
    }

    /**
     * Place Details 조회 (New API)
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse? = withContext(Dispatchers.IO) {
        try {
            googlePlacesRestClient.get()
                .uri("/v1/places/{placeId}?languageCode=ko", placeId)
                .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                .header("X-Goog-FieldMask", buildPlaceDetailsFieldMask())
                .retrieve()
                .body(PlaceDetailsResponse::class.java)
        } catch (e: Exception) {
            logger.error(e) { "장소 상세 정보 조회 실패: placeId=$placeId" }
            null
        }
    }
    
    /**
     * Nearby Search - 주변 지하철역 검색 (New API)
     */
    suspend fun searchNearby(latitude: Double, longitude: Double, radius: Double = 1000.0): org.depromeet.team3.place.model.NearbySearchResponse? = withContext(Dispatchers.IO) {
        try {
            val request = org.depromeet.team3.place.model.NearbySearchRequest(
                includedTypes = listOf("subway_station", "transit_station", "train_station"),
                maxResultCount = 1,
                locationRestriction = org.depromeet.team3.place.model.NearbySearchRequest.LocationRestriction(
                    circle = org.depromeet.team3.place.model.NearbySearchRequest.LocationRestriction.Circle(
                        center = org.depromeet.team3.place.model.NearbySearchRequest.LocationRestriction.Circle.Center(
                            latitude = latitude,
                            longitude = longitude
                        ),
                        radius = radius
                    )
                ),
                rankPreference = "DISTANCE"
            )
            
            googlePlacesRestClient.post()
                .uri("/v1/places:searchNearby")
                .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                .header("X-Goog-FieldMask", "places.id,places.displayName,places.location")
                .header("Accept-Language", "ko")
                .body(request)
                .retrieve()
                .body(org.depromeet.team3.place.model.NearbySearchResponse::class.java)
        } catch (e: Exception) {
            logger.error(e) { "주변 역 검색 실패: lat=$latitude, lng=$longitude" }
            null
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
            "places.currentOpeningHours"
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
