package org.depromeet.team3.place.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Google Places API (New) Text Search 응답
 */
data class PlacesTextSearchResponse(
    val places: List<Place>?
) {
    data class Place(
        val id: String,
        @JsonProperty("displayName")
        val displayName: DisplayName,
        @JsonProperty("formattedAddress")
        val formattedAddress: String,
        val rating: Double? = null,
        @JsonProperty("userRatingCount")
        val userRatingCount: Int? = null,
        @JsonProperty("currentOpeningHours")
        val currentOpeningHours: OpeningHours? = null,
        val types: List<String>? = null
    ) {
        data class DisplayName(
            val text: String,
            val languageCode: String? = null
        )
        
        data class OpeningHours(
            @JsonProperty("openNow")
            val openNow: Boolean? = null
        )
    }
}
