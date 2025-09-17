package org.depromeet.team3.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "kakao")
data class KakaoProperties(
    var clientId: String = "",
    var redirectUri: Any = "",
    var tokenUri: String = "https://kauth.kakao.com/oauth/token",
    var userInfoUri: String = "https://kapi.kakao.com/v2/user/me"
) {
    // 단일 redirect URI를 반환하는 메서드 (기존 호환성)
    fun getRedirectUri(): String {
        return when (redirectUri) {
            is String -> redirectUri as String
            is List<*> -> (redirectUri as List<*>).firstOrNull()?.toString() ?: ""
            else -> ""
        }
    }

    // 모든 redirect URI를 반환하는 메서드
    fun getAllowedRedirectUris(): Set<String> {
        return when (redirectUri) {
            is String -> setOf(redirectUri as String)
            is List<*> -> (redirectUri as List<*>).mapNotNull { it?.toString() }.toSet()
            else -> emptySet()
        }
    }
}
