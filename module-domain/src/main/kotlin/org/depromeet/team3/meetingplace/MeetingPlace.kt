package org.depromeet.team3.meetingplace

import org.depromeet.team3.common.BaseTimeDomain
import java.time.LocalDateTime

data class MeetingPlace(
    val id: Long? = null,
    val meetingId: Long,
    val placeId: Long,
    val likeCount: Int = 0,
    val likedUserIds: Set<Long> = emptySet(),
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
) : BaseTimeDomain(createdAt, updatedAt) {
    
    fun toggleLike(userId: Long): MeetingPlace {
        val newLikedUsers = if (userId in likedUserIds) {
            likedUserIds - userId  // 좋아요 취소
        } else {
            likedUserIds + userId  // 좋아요 추가
        }
        
        return this.copy(
            likedUserIds = newLikedUsers,
            likeCount = newLikedUsers.size
        )
    }
    
    fun isLikedBy(userId: Long): Boolean = userId in likedUserIds
}