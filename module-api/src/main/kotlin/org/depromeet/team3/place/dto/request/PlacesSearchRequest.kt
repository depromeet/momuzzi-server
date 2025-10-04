package org.depromeet.team3.place.dto.request

data class PlacesSearchRequest(
    val query: String,
    val maxResults: Int = 5,
    val pageToken: String? = null
)
