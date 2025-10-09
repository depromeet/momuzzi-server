package org.depromeet.team3.mapper

import org.depromeet.team3.menu.MenuJpaRepository
import org.depromeet.team3.place.Place
import org.depromeet.team3.place.PlaceEntity
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class PlaceMapper(
    private val menuJpaRepository: MenuJpaRepository,
    @Lazy private val menuMapper: MenuMapper
) : DomainMapper<Place, PlaceEntity> {
    
    override fun toDomain(entity: PlaceEntity): Place {
        val menus = entity.menus.map { menuMapper.toDomain(it) }.toMutableList()

        return Place(
            id = entity.id!!,
            name = entity.name,
            category = entity.category,
            rating = entity.rating,
            reviewCount = entity.reviewCount,
            address = entity.address,
            closestStation = entity.closestStation,
            workingHours = entity.workingHours,
            phoneNo = entity.phoneNo,
            descriptions = entity.descriptions,
            isDeleted = entity.isDeleted,
            menus = menus,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Place): PlaceEntity {
        val menuEntities = domain.id?.let(menuJpaRepository::findAllByPlaceId) ?: emptyList()

        return PlaceEntity(
            id = domain.id,
            name = domain.name,
            category = domain.category,
            rating = domain.rating,
            reviewCount = domain.reviewCount,
            address = domain.address,
            closestStation = domain.closestStation,
            workingHours = domain.workingHours,
            phoneNo = domain.phoneNo,
            descriptions = domain.descriptions,
            isDeleted = domain.isDeleted,
            menus = menuEntities.toMutableList()
        )
    }
}
