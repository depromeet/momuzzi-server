package org.depromeet.team3.place

import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesSearchResponse
import org.springframework.stereotype.Repository

@Repository
class PlaceQuery(
    private val googlePlacesClient: GooglePlacesClient
) {
    /**
     * 텍스트 검색
     */
    suspend fun textSearch(query: String, maxResults: Int = 5): PlacesSearchResponse? {
        return googlePlacesClient.textSearch(query, maxResults)
    }

    /**
     * Place Details 조회
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse? {
        return googlePlacesClient.getPlaceDetails(placeId)
    }
}
