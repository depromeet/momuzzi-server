package org.depromeet.team3.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.depromeet.team3.meeting.MeetingEntity
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceEntity
import org.depromeet.team3.place.PlaceEntity
import org.springframework.stereotype.Component

@Component
class MeetingPlaceMapper(
    private val meetingJpaRepository: org.depromeet.team3.meeting.MeetingJpaRepository,
    private val placeJpaRepository: org.depromeet.team3.place.PlaceJpaRepository,
    private val objectMapper: ObjectMapper
) {
    fun toDomain(entity: MeetingPlaceEntity): MeetingPlace {
        val likedUserIds = try {
            objectMapper.readValue<List<Long>>(entity.likedUserIds).toSet()
        } catch (e: Exception) {
            emptySet()
        }
        
        return MeetingPlace(
            id = entity.id,
            meetingId = entity.meeting.id!!,
            placeId = entity.place.id!!,
            likeCount = entity.likeCount,
            likedUserIds = likedUserIds,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: MeetingPlace): MeetingPlaceEntity {
        val meeting = meetingJpaRepository.findById(domain.meetingId)
            .orElseThrow { IllegalArgumentException("Meeting not found: ${domain.meetingId}") }
        val place = placeJpaRepository.findById(domain.placeId)
            .orElseThrow { IllegalArgumentException("Place not found: ${domain.placeId}") }

        val likedUserIdsJson = objectMapper.writeValueAsString(domain.likedUserIds.toList())

        return MeetingPlaceEntity(
            id = domain.id,
            meeting = meeting,
            place = place,
            likeCount = domain.likeCount,
            likedUserIds = likedUserIdsJson
        )
    }
}
