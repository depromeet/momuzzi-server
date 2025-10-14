package org.depromeet.team3.place.dto.response

data class PlaceLikeResponse(
    val isLiked: Boolean,
    val likeCount: Int,
    val message: String
)
