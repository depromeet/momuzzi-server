package org.depromeet.team3.sattion.application

import org.depromeet.team3.sattion.dto.response.StationResponse
import org.depromeet.team3.station.StationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetStationService(
    private val stationRepository: StationRepository
) {

    @Transactional(readOnly = true)
    fun getAllStations(): List<StationResponse> {
        return stationRepository.findAll().map { StationResponse(it.id!!, it.name) }
    }
}