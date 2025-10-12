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
            googlePlaceId = entity.googlePlaceId,
            name = entity.name,
            address = entity.address,
            rating = entity.rating,
            userRatingsTotal = entity.userRatingsTotal,
            openNow = entity.openNow,
            link = entity.link,
            weekdayText = entity.weekdayText,
            topReviewRating = entity.topReviewRating,
            topReviewText = entity.topReviewText,
            priceRangeStart = entity.priceRangeStart,
            priceRangeEnd = entity.priceRangeEnd,
            addressDescriptor = entity.addressDescriptor,
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
            googlePlaceId = domain.googlePlaceId,
            name = domain.name,
            address = domain.address,
            rating = domain.rating,
            userRatingsTotal = domain.userRatingsTotal,
            openNow = domain.openNow,
            link = domain.link,
            weekdayText = domain.weekdayText,
            topReviewRating = domain.topReviewRating,
            topReviewText = domain.topReviewText,
            priceRangeStart = domain.priceRangeStart,
            priceRangeEnd = domain.priceRangeEnd,
            addressDescriptor = domain.addressDescriptor,
            isDeleted = domain.isDeleted,
            menus = menuEntities.toMutableList()
        )
    }
}
