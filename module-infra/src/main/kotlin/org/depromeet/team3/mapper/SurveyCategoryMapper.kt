package org.depromeet.team3.mapper

import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryEntity
import org.depromeet.team3.surveycategory.SurveyCategoryJpaRepository
import org.springframework.stereotype.Component

@Component
class SurveyCategoryMapper(
    private val surveyCategoryJpaRepository: SurveyCategoryJpaRepository
) : DomainMapper<SurveyCategory, SurveyCategoryEntity> {
    
    override fun toDomain(entity: SurveyCategoryEntity): SurveyCategory {
        return SurveyCategory(
            id = entity.id,
            parentId = entity.parent?.id,
            type = entity.type,
            level = entity.level,
            name = entity.name,
            order = entity.order,
            isDeleted = entity.isDeleted,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: SurveyCategory): SurveyCategoryEntity {
        val parentEntity = domain.parentId?.let { 
            surveyCategoryJpaRepository.findById(it)
                .orElseThrow { IllegalArgumentException("Parent SurveyCategory not found with id: ${domain.parentId}") }
        }
        
        return SurveyCategoryEntity(
            id = domain.id,
            parent = parentEntity,
            type = domain.type,
            level = domain.level,
            name = domain.name,
            order = domain.order,
            isDeleted = domain.isDeleted
        )
    }
}