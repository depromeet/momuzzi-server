package org.depromeet.team3.auth.application

import org.depromeet.team3.auth.UserCommandRepository
import org.depromeet.team3.auth.UserQueryRepository
import org.depromeet.team3.auth.command.RefreshTokenCommand
import org.depromeet.team3.auth.dto.TokenResponse
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 토큰 갱신 Command Service
 * 읽기와 쓰기 작업 모두 수행
 */
@Service
class UpdateTokenService(
    private val userQueryRepository: UserQueryRepository,
    private val userCommandRepository: UserCommandRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun refresh(command: RefreshTokenCommand): TokenResponse {
        val refreshToken = command.refreshToken
        
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw AuthException(ErrorCode.REFRESH_TOKEN_INVALID)
        }

        // 2. 사용자 정보 조회 (Query Repository 사용)
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)?.toLongOrNull()
            ?: throw AuthException(ErrorCode.TOKEN_USER_ID_INVALID)

        val user = userQueryRepository.findById(userId)
            ?: throw AuthException(ErrorCode.USER_NOT_FOUND_FOR_TOKEN)

        // 3. 토큰 일치성 검증
        if (user.refreshToken != refreshToken) {
            throw AuthException(ErrorCode.REFRESH_TOKEN_MISMATCH)
        }

        // 4. 새로운 토큰 생성 및 저장 (Command Repository 사용)
        val accessToken = jwtTokenProvider.generateAccessToken(userId, user.email)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(userId)
        
        val updatedUser = user.copy(
            refreshToken = newRefreshToken,
            updatedAt = LocalDateTime.now()
        )
        userCommandRepository.save(updatedUser)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = newRefreshToken
        )
    }
}
