package org.depromeet.team3.place.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.depromeet.team3.common.GooglePlacesApiProperties
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesSearchResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GooglePlacesClient(
    private val googlePlacesRestClient: RestClient,
    private val googlePlacesApiProperties: GooglePlacesApiProperties,
) {

    private val logger = KotlinLogging.logger { GooglePlacesClient::class.java.name }

    /**
     * 텍스트 검색
     */
    fun textSearch(query: String, maxResults: Int = 5): PlacesSearchResponse? {
        return try {
            googlePlacesRestClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/textsearch/json")
                        .queryParam("query", query)
                        .queryParam("language", "ko")
                        .queryParam("key", googlePlacesApiProperties.apiKey)
                        .build()
                }
                .retrieve()
                .body(PlacesSearchResponse::class.java)
                ?.let { response ->
                    response.copy(results = response.results.take(maxResults))
                }
        } catch (e: Exception) {
            logger.error(e) { "Failed to text search: query=$query" }
            null
        }
    }

    /**
     * Place Details 조회 (전체 정보)
     */
    fun getPlaceDetails(placeId: String): PlaceDetailsResponse? {
        return try {
            googlePlacesRestClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("fields", "place_id,name,formatted_address,website,rating,user_ratings_total,opening_hours,photos,url,types,reviews")
                        .queryParam("language", "ko")
                        .queryParam("key", googlePlacesApiProperties.apiKey)
                        .build()
                }
                .retrieve()
                .body(PlaceDetailsResponse::class.java)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get place details: placeId=$placeId" }
            null
        }
    }
}
