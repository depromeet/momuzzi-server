package org.depromeet.team3.mapper

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.menu.Menu
import org.depromeet.team3.menu.MenuEntity
import org.depromeet.team3.place.PlaceJpaRepository
import org.depromeet.team3.place.exception.PlaceException
import org.springframework.stereotype.Component

@Component
class MenuMapper(
    private val placeJpaRepository: PlaceJpaRepository
) : DomainMapper<Menu, MenuEntity> {
    
    override fun toDomain(entity: MenuEntity): Menu {
        return Menu(
            id = entity.id!!,
            placeId = entity.place.id!!,
            name = entity.name,
            category = entity.category,
            price = entity.price,
            isDeleted = entity.isDeleted,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Menu): MenuEntity {
        val placeEntity = placeJpaRepository.findById(domain.placeId)
            .orElseThrow { 
                PlaceException(
                    errorCode = ErrorCode.PLACE_NOT_FOUND,
                    detail = mapOf("placeId" to domain.placeId)
                )
            }

        return MenuEntity(
            id = domain.id,
            name = domain.name,
            category = domain.category,
            price = domain.price,
            isDeleted = domain.isDeleted,
            place = placeEntity
        )
    }
}
