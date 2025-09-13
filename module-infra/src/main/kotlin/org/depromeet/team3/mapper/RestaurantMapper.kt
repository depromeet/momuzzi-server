package org.depromeet.team3.mapper

import org.depromeet.team3.restaurant.Restaurant
import org.depromeet.team3.restaurant.RestaurantEntity
import org.springframework.stereotype.Component

@Component
class RestaurantMapper : DomainMapper<Restaurant, RestaurantEntity> {
    
    override fun toDomain(entity: RestaurantEntity): Restaurant {
        return Restaurant(
            id = entity.id,
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
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    override fun toEntity(domain: Restaurant): RestaurantEntity {
        val entity = RestaurantEntity(
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
            isDeleted = domain.isDeleted
        )
        // BaseTimeEntity의 createdAt은 자동으로 설정되므로 별도 설정 불필요
        // updatedAt은 필요시 updateTimestamp() 메서드 호출
        return entity
    }
}
