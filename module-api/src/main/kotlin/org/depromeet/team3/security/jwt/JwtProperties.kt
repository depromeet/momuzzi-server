package org.depromeet.team3.security.jwt

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtProperties(
    @Value("\${jwt.secret}")
    val secretKey: String,

    @Value("\${jwt.access-token-validity}") // 1시간
    val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-validity}") // 7일
    val refreshTokenExpiration: Long,
)
