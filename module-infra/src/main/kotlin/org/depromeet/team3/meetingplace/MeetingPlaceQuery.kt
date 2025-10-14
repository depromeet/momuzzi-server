package org.depromeet.team3.meetingplace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.mapper.MeetingPlaceMapper
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.place.PlaceJpaRepository
import org.depromeet.team3.place.exception.PlaceException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MeetingPlaceQuery(
    private val meetingPlaceJpaRepository: MeetingPlaceJpaRepository,
    private val meetingPlaceMapper: MeetingPlaceMapper,
    private val meetingJpaRepository: MeetingJpaRepository,
    private val placeJpaRepository: PlaceJpaRepository
) : MeetingPlaceRepository {

    @Transactional
    override suspend fun save(meetingPlace: MeetingPlace): MeetingPlace = withContext(Dispatchers.IO) {
        // Meeting과 Place 조회
        val meeting = meetingJpaRepository.findById(meetingPlace.meetingId)
            .orElseThrow { 
                MeetingException(
                    errorCode = ErrorCode.MEETING_NOT_FOUND,
                    detail = mapOf("meetingId" to meetingPlace.meetingId)
                )
            }
        val place = placeJpaRepository.findById(meetingPlace.placeId)
            .orElseThrow { 
                PlaceException(
                    errorCode = ErrorCode.PLACE_NOT_FOUND,
                    detail = mapOf("placeId" to meetingPlace.placeId)
                )
            }
        
        // Entity 변환 및 저장
        val entity = meetingPlaceMapper.toEntity(meetingPlace, meeting, place)
        val saved = meetingPlaceJpaRepository.save(entity)
        meetingPlaceMapper.toDomain(saved)
    }

    @Transactional
    override suspend fun saveAll(meetingPlaces: List<MeetingPlace>): List<MeetingPlace> = withContext(Dispatchers.IO) {
        // Meeting과 Place를 미리 조회 (N+1 방지)
        val meetingIds = meetingPlaces.map { it.meetingId }.distinct()
        val placeIds = meetingPlaces.map { it.placeId }.distinct()
        
        val meetings = meetingJpaRepository.findAllById(meetingIds).associateBy { it.id }
        val places = placeJpaRepository.findAllById(placeIds).associateBy { it.id }
        
        // Entity 변환
        val entities = meetingPlaces.map { meetingPlace ->
            val meeting = meetings[meetingPlace.meetingId]
                ?: throw MeetingException(
                    errorCode = ErrorCode.MEETING_NOT_FOUND,
                    detail = mapOf("meetingId" to meetingPlace.meetingId)
                )
            val place = places[meetingPlace.placeId]
                ?: throw PlaceException(
                    errorCode = ErrorCode.PLACE_NOT_FOUND,
                    detail = mapOf("placeId" to meetingPlace.placeId)
                )
            
            meetingPlaceMapper.toEntity(meetingPlace, meeting, place)
        }
        
        // 저장
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
