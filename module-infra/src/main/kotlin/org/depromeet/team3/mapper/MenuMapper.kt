package org.depromeet.team3.mapper

import org.depromeet.team3.place.Menu
import org.depromeet.team3.place.MenuEntity
import org.depromeet.team3.restaurant.RestaurantJpaRepository
import org.springframework.stereotype.Component

@Component
class MenuMapper(
    private val restaurantJpaRepository: RestaurantJpaRepository
) : DomainMapper<Menu, MenuEntity> {
    
    override fun toDomain(entity: MenuEntity): Menu {
        return Menu(
            id = entity.id!!,
            restaurantId = entity.restaurant.id!!, // 직접 접근
            name = entity.name,
            category = entity.category,
            price = entity.price,
            isDeleted = entity.isDeleted,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Menu): MenuEntity {
        val restaurantEntity = restaurantJpaRepository.findById(domain.restaurantId)
            .orElseThrow { IllegalArgumentException(
                "could not find restaurant with id: ${domain.restaurantId}")
            }

        return MenuEntity(
            id = domain.id,
            name = domain.name,
            category = domain.category,
            price = domain.price,
            isDeleted = domain.isDeleted,
            restaurant = restaurantEntity
        )
    }
}
