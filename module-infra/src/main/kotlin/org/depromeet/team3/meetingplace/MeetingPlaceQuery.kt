package org.depromeet.team3.meetingplace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.mapper.MeetingPlaceMapper
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MeetingPlaceQuery(
    private val meetingPlaceJpaRepository: MeetingPlaceJpaRepository,
    private val meetingPlaceMapper: MeetingPlaceMapper
) : MeetingPlaceRepository {

    @Transactional
    override suspend fun save(meetingPlace: MeetingPlace): MeetingPlace = withContext(Dispatchers.IO) {
        val entity = meetingPlaceMapper.toEntity(meetingPlace)
        val saved = meetingPlaceJpaRepository.save(entity)
        meetingPlaceMapper.toDomain(saved)
    }

    @Transactional
    override suspend fun saveAll(meetingPlaces: List<MeetingPlace>): List<MeetingPlace> = withContext(Dispatchers.IO) {
        val entities = meetingPlaces.map { meetingPlaceMapper.toEntity(it) }
        val saved = meetingPlaceJpaRepository.saveAll(entities)
        saved.map { meetingPlaceMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override suspend fun findByMeetingId(meetingId: Long): List<MeetingPlace> = withContext(Dispatchers.IO) {
        meetingPlaceJpaRepository.findByMeetingId(meetingId)
            .map { meetingPlaceMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override suspend fun findByMeetingIdAndPlaceId(meetingId: Long, placeId: Long): MeetingPlace? =
        withContext(Dispatchers.IO) {
            meetingPlaceJpaRepository.findByMeetingIdAndPlaceId(meetingId, placeId)
                ?.let { meetingPlaceMapper.toDomain(it) }
        }

    @Transactional
    override suspend fun deleteByMeetingId(meetingId: Long) = withContext(Dispatchers.IO) {
        meetingPlaceJpaRepository.deleteByMeetingId(meetingId)
    }

    @Transactional(readOnly = true)
    override suspend fun existsByMeetingIdAndPlaceId(meetingId: Long, placeId: Long): Boolean =
        withContext(Dispatchers.IO) {
            meetingPlaceJpaRepository.existsByMeetingIdAndPlaceId(meetingId, placeId)
        }
}
