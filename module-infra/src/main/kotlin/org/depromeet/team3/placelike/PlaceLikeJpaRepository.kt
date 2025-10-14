package org.depromeet.team3.placelike

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType

@Repository
interface PlaceLikeJpaRepository : JpaRepository<PlaceLikeEntity, Long> {
    fun findByMeetingPlaceIdAndUserId(meetingPlaceId: Long, userId: Long): PlaceLikeEntity?
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pl FROM PlaceLikeEntity pl WHERE pl.meetingPlace.id = :meetingPlaceId AND pl.userId = :userId")
    fun findByMeetingPlaceIdAndUserIdForUpdate(
        @Param("meetingPlaceId") meetingPlaceId: Long,
        @Param("userId") userId: Long
    ): PlaceLikeEntity?
    
    fun countByMeetingPlaceId(meetingPlaceId: Long): Long
    fun findByMeetingPlaceIdIn(meetingPlaceIds: List<Long>): List<PlaceLikeEntity>
}