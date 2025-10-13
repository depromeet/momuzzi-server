package org.depromeet.team3.mapper

import org.depromeet.team3.meetingplace.MeetingPlaceJpaRepository
import org.depromeet.team3.placelike.PlaceLike
import org.depromeet.team3.placelike.PlaceLikeEntity
import org.springframework.stereotype.Component

@Component
class PlaceLikeMapper(
    private val meetingPlaceJpaRepository: MeetingPlaceJpaRepository
) {
    fun toDomain(entity: PlaceLikeEntity): PlaceLike {
        return PlaceLike(
            id = entity.id,
            meetingPlaceId = entity.meetingPlace.id!!,
            userId = entity.userId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: PlaceLike): PlaceLikeEntity {
        val meetingPlace = meetingPlaceJpaRepository.findById(domain.meetingPlaceId)
            .orElseThrow { IllegalArgumentException("MeetingPlace not found: ${domain.meetingPlaceId}") }

        return PlaceLikeEntity(
            id = domain.id,
            meetingPlace = meetingPlace,
            userId = domain.userId
        )
    }
}
