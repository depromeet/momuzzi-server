package org.depromeet.team3.place.client

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    suspend fun textSearch(query: String, maxResults: Int = 5): PlacesSearchResponse? = withContext(Dispatchers.IO) {
        try {
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
            logger.error(e) { "텍스트 검색 실패: query=$query" }
            null
        }
    }

    /**
     * Place Details 조회
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse? = withContext(Dispatchers.IO) {
        try {
            googlePlacesRestClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("fields", "place_id,name,formatted_address,rating,user_ratings_total,opening_hours,photos,url,types,reviews")
                        .queryParam("language", "ko")
                        .queryParam("key", googlePlacesApiProperties.apiKey)
                        .build()
                }
                .retrieve()
                .body(PlaceDetailsResponse::class.java)
        } catch (e: Exception) {
            logger.error(e) { "장소 상세 정보 조회 실패: placeId=$placeId" }
            null
        }
    }
}
