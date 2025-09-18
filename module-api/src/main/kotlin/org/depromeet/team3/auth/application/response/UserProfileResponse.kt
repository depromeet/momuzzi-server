package org.depromeet.team3.auth.application.response

data class UserProfileResponse(
    val email: String,
    val nickname: String,
    val profileImage: String?
)
