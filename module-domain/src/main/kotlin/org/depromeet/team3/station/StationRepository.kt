package org.depromeet.team3.station

interface StationRepository {

    fun findAll(): List<Station>
    fun findAllById(ids: List<Long>) : List<Station>
    fun findById(id: Long): Station?
}