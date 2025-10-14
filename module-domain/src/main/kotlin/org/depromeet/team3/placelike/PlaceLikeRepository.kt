package org.depromeet.team3.placelike

interface PlaceLikeRepository {
    suspend fun save(placeLike: PlaceLike): PlaceLike
    suspend fun findByMeetingPlaceIdAndUserId(meetingPlaceId: Long, userId: Long): PlaceLike?
    suspend fun findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId: Long, userId: Long): PlaceLike?
    suspend fun deleteByMeetingPlaceIdAndUserId(meetingPlaceId: Long, userId: Long)
    suspend fun countByMeetingPlaceId(meetingPlaceId: Long): Long
    suspend fun findByMeetingPlaceIds(meetingPlaceIds: List<Long>): List<PlaceLike>
}