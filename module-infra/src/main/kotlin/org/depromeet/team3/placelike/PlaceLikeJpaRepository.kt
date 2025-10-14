package org.depromeet.team3.placelike

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository

@Repository
interface PlaceLikeJpaRepository : JpaRepository<PlaceLikeEntity, Long> {
    fun findByMeetingPlaceIdAndUserId(meetingPlaceId: Long, userId: Long): PlaceLikeEntity?
    fun countByMeetingPlaceId(meetingPlaceId: Long): Long
    fun findByMeetingPlaceIdIn(meetingPlaceIds: List<Long>): List<PlaceLikeEntity>
}
