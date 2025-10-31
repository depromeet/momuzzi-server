package org.depromeet.team3.mapper

import org.depromeet.team3.placelike.PlaceLike
import org.depromeet.team3.placelike.PlaceLikeEntity
import org.springframework.stereotype.Component

@Component
class PlaceLikeMapper {
    fun toDomain(entity: PlaceLikeEntity): PlaceLike {
        return PlaceLike(
            id = entity.id,
            meetingPlaceId = entity.meetingPlaceId,
            userId = entity.userId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: PlaceLike): PlaceLikeEntity {
        return PlaceLikeEntity(
            id = domain.id,
            meetingPlaceId = domain.meetingPlaceId,
            userId = domain.userId
        )
    }
}
