package org.depromeet.team3.mapper

import org.depromeet.team3.meeting.MeetingEntity
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceEntity
import org.depromeet.team3.place.PlaceEntity
import org.springframework.stereotype.Component

@Component
class MeetingPlaceMapper {
    fun toDomain(entity: MeetingPlaceEntity): MeetingPlace {
        return MeetingPlace(
            id = entity.id,
            meetingId = entity.meeting.id!!,
            placeId = entity.place.id!!,
            likeCount = entity.likeCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(
        domain: MeetingPlace, 
        meeting: MeetingEntity, 
        place: PlaceEntity
    ): MeetingPlaceEntity {
        return MeetingPlaceEntity(
            id = domain.id,
            meeting = meeting,
            place = place,
            likeCount = domain.likeCount
        )
    }
}
