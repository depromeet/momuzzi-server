package org.depromeet.team3.station

import org.depromeet.team3.mapper.StationMapper
import org.springframework.stereotype.Repository

@Repository
class StationQuery(
    private val stationMapper: StationMapper,
    private val stationJpaRepository: StationJpaRepository
) : StationRepository {

    override fun findAll(): List<Station> {
        return stationJpaRepository.findAll().map { stationMapper.toDomain(it) }
    }
    
    override fun findById(id: Long): Station? {
        return stationJpaRepository.findById(id).orElse(null)?.let { stationMapper.toDomain(it) }
    }
}