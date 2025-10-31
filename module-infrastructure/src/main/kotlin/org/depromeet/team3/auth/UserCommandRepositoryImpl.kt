package org.depromeet.team3.auth

import jakarta.transaction.Transactional
import org.depromeet.team3.auth.exception.UserException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.mapper.UserMapper
import org.springframework.stereotype.Repository

/**
 * User Command Repository 구현체
 * 쓰기 작업만 처리
 */
@Repository
@Transactional
class UserCommandRepositoryImpl(
    private val userJpaRepository: UserRepository,
    private val userMapper: UserMapper
) : UserCommandRepository {

    override fun save(user: User): User {
        val entity = userMapper.toEntity(user)
        val savedEntity = userJpaRepository.save(entity)
        return userMapper.toDomain(savedEntity)
    }

    override fun delete(user: User) {
        user.id?.let { userId ->
            userJpaRepository.deleteById(userId)
        } ?: throw UserException(
            errorCode = ErrorCode.USER_ID_REQUIRED,
            message = "삭제할 사용자의 ID가 없습니다"
        )
    }
}
