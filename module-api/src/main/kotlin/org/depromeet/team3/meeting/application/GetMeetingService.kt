package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.response.MeetingResponse
import org.depromeet.team3.station.StationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMeetingService(
    private val meetingRepository: MeetingRepository,
    private val stationRepository: StationRepository
) {

    @Transactional(readOnly = true)
    operator fun invoke(userId: Long): List<MeetingResponse> {
        val meetings = meetingRepository.findMeetingsByUserId(userId)
        val stationIds = meetings.mapNotNull { it.stationId }.distinct()
        val stationMap = stationRepository.findAllById(stationIds)
            .associateBy { it.id }

        return meetings.map { meeting ->
            val stationName = meeting.stationId?.let {
                stationMap[it]?.name
            } ?: ""

            MeetingResponse(
                id = meeting.id!!,
                name = meeting.name,
                hostUserId = meeting.hostUserId,
                attendeeCount = meeting.attendeeCount,
                isClosed = meeting.isClosed,
                stationName = stationName,
                endAt = meeting.endAt!!,
                createdAt = meeting.createdAt!!,
                updatedAt = meeting.updatedAt
            )
        }
    }
}