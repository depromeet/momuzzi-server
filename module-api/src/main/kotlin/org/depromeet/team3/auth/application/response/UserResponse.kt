package org.depromeet.team3.auth.application.response

data class UserResponse (
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val accessToken: String,
    val refreshToken: String
)

