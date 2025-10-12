package org.depromeet.team3.place.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Google Places API (New) Text Search 요청
 */
data class PlacesTextSearchRequest(
    @JsonProperty("textQuery")
    val textQuery: String,
    @JsonProperty("languageCode")
    val languageCode: String = "ko",
    @JsonProperty("maxResultCount")
    val maxResultCount: Int = 10
)
