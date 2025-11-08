package org.depromeet.team3.place.dto.request

data class PlacesSearchRequest(
    val query: String? = null,
    val pageToken: String? = null,
    val meetingId: Long? = null,
    val userId: Long? = null
)
