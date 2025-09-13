package org.depromeet.team3.common

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "api.naver.map")
data class NaverApiProperties(
    val clientId: String,
    val clientSecret: String,
    val geocodingBaseUrl: String,
    val localSearchBaseUrl: String
)