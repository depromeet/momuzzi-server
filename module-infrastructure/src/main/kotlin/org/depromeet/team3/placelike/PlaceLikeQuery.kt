package org.depromeet.team3.placelike

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.mapper.PlaceLikeMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlaceLikeQuery(
    private val placeLikeJpaRepository: PlaceLikeJpaRepository,
    private val placeLikeMapper: PlaceLikeMapper
) : PlaceLikeRepository {

    @Transactional
    override suspend fun save(placeLike: PlaceLike): PlaceLike  {
        val entity = placeLikeMapper.toEntity(placeLike)
        val saved = placeLikeJpaRepository.save(entity)
        return placeLikeMapper.toDomain(saved)
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
    ): Unit = withContext(Dispatchers.IO) {
        placeLikeJpaRepository.findByMeetingPlaceIdAndUserId(meetingPlaceId, userId)
            ?.let { placeLikeJpaRepository.delete(it) }
        Unit
    }

    @Transactional(readOnly = true)
    override suspend fun countByMeetingPlaceId(meetingPlaceId: Long): Long =
        withContext(Dispatchers.IO) {
            placeLikeJpaRepository.countByMeetingPlaceId(meetingPlaceId)
        }

    @Transactional(readOnly = true)
    override suspend fun findByMeetingPlaceIds(meetingPlaceIds: List<Long>): List<PlaceLike> =
        withContext(Dispatchers.IO) {
            if (meetingPlaceIds.isEmpty()) {
                emptyList()
            } else {
                placeLikeJpaRepository.findByMeetingPlaceIdIn(meetingPlaceIds)
                    .map { placeLikeMapper.toDomain(it) }
            }
        }
}
