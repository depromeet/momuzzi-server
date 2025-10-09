package org.depromeet.team3.auth

/**
 * User Command Repository
 * 쓰기 작업만 담당
 */
interface UserCommandRepository {
    fun save(user: User): User
    fun delete(user: User)
}
