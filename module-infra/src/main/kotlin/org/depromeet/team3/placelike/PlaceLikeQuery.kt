package org.depromeet.team3.placelike

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.placelike.PlaceLike
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlaceLikeQuery(
    private val placeLikeJpaRepository: PlaceLikeJpaRepository,
    private val placeLikeMapper: org.depromeet.team3.mapper.PlaceLikeMapper
) : PlaceLikeRepository {

    @Transactional
    override suspend fun save(placeLike: PlaceLike): PlaceLike = withContext(Dispatchers.IO) {
        val entity = placeLikeMapper.toEntity(placeLike)
        val saved = placeLikeJpaRepository.save(entity)
        placeLikeMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override suspend fun findByMeetingPlaceIdAndUserId(
        meetingPlaceId: Long,
        userId: Long
    ): PlaceLike? = withContext(Dispatchers.IO) {
        placeLikeJpaRepository.findByMeetingPlaceIdAndUserId(meetingPlaceId, userId)
            ?.let { placeLikeMapper.toDomain(it) }
    }

    @Transactional
    override suspend fun deleteByMeetingPlaceIdAndUserId(
        meetingPlaceId: Long,
        userId: Long
    ) = withContext(Dispatchers.IO) {
        placeLikeJpaRepository.deleteByMeetingPlaceIdAndUserId(meetingPlaceId, userId)
    }

    @Transactional(readOnly = true)
    override suspend fun countByMeetingPlaceId(meetingPlaceId: Long): Long =
        withContext(Dispatchers.IO) {
            placeLikeJpaRepository.countByMeetingPlaceId(meetingPlaceId)
        }

    @Transactional(readOnly = true)
    override suspend fun findByMeetingPlaceIds(meetingPlaceIds: List<Long>): List<PlaceLike> =
        withContext(Dispatchers.IO) {
            placeLikeJpaRepository.findByMeetingPlaceIdIn(meetingPlaceIds)
                .map { placeLikeMapper.toDomain(it) }
        }
}
