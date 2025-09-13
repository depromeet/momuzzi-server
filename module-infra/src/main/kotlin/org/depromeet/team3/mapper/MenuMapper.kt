package org.depromeet.team3.mapper

import org.depromeet.team3.menu.Menu
import org.depromeet.team3.menu.MenuEntity
import org.depromeet.team3.restaurant.RestaurantJpaRepository
import org.springframework.stereotype.Component

@Component
class MenuMapper(
    private val restaurantMapper: RestaurantMapper,
    private val restaurantJpaRepository: RestaurantJpaRepository
) : DomainMapper<Menu, MenuEntity> {
    
    override fun toDomain(entity: MenuEntity): Menu {
        return Menu(
            id = entity.id!!,
            restaurantId = restaurantMapper.toDomain(entity.restaurant).id!!,
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
