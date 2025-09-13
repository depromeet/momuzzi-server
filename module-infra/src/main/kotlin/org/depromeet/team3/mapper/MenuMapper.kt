package org.depromeet.team3.mapper

import org.depromeet.team3.menu.Menu
import org.depromeet.team3.menu.MenuEntity
import org.springframework.stereotype.Component

@Component
class MenuMapper : DomainMapper<Menu, MenuEntity> {
    
    override fun toDomain(entity: MenuEntity): Menu {
        return Menu(
            id = entity.id,
            restaurantId = entity.restaurantId,
            name = entity.name,
            category = entity.category,
            price = entity.price,
            isDeleted = entity.isDeleted,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Menu): MenuEntity {
        val entity = MenuEntity(
            id = domain.id,
            restaurantId = domain.restaurantId,
            name = domain.name,
            category = domain.category,
            price = domain.price,
            isDeleted = domain.isDeleted
        )
        // BaseTimeEntity의 createdAt은 자동으로 설정되므로 별도 설정 불필요
        // updatedAt은 필요시 updateTimestamp() 메서드 호출
        return entity
    }
}
