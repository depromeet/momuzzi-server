package org.depromeet.team3.naver.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverLocalSearchResponse(
    val lastBuildDate: String,
    val total: Int,
    val start: Int,
    val display: Int,
    val items: List<NaverLocalSearchItem>
)

data class NaverLocalSearchItem(
    val title: String,
    val link: String,
    val category: String,
    val description: String,
    val telephone: String?,
    val address: String,
    val roadAddress: String,
    @JsonProperty("mapx")
    val mapX: String,
    @JsonProperty("mapy")
    val mapY: String
)
