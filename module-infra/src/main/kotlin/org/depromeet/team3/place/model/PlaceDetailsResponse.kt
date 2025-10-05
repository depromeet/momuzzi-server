package org.depromeet.team3.place.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *  구글 플레이스 Place Details API 응답
 */
data class PlaceDetailsResponse(
    val result: PlaceDetail? = null,
    val status: String
) {
    data class PlaceDetail(
        val website: String? = null,
        @JsonProperty("opening_hours")
        val openingHours: OpeningHours? = null,
        val reviews: List<Review>? = null,
        val photos: List<Photo>? = null
    ) {
        data class OpeningHours(
            @JsonProperty("weekday_text")
            val weekdayText: List<String>? = null
        )
        
        data class Review(
            @JsonProperty("author_name")
            val authorName: String,
            val rating: Double,
            @JsonProperty("relative_time_description")
            val relativeTimeDescription: String,
            val text: String,
            val time: Long
        )
        
        data class Photo(
            @JsonProperty("photo_reference")
            val photoReference: String,
            val height: Int,
            val width: Int
        )
    }
}
