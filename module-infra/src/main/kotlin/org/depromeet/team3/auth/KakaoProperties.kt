package org.depromeet.team3.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class KakaoProperties(
    @Value("\${kakao.client-id}")
    val clientId: String,
    @Value("\${kakao.client-secret:}")
    val clientSecret: String?
)
