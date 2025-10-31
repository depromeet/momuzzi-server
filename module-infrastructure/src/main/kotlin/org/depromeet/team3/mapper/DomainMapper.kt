package org.depromeet.team3.mapper

interface DomainMapper<D, E> {
    fun toDomain(entity: E): D
    fun toEntity(domain: D): E
}
