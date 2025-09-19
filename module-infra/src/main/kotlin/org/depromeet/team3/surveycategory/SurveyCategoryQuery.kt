package org.depromeet.team3.surveycategory

import org.depromeet.team3.common.enums.SurveyCategoryType
import org.depromeet.team3.mapper.SurveyCategoryMapper
import org.springframework.stereotype.Repository

@Repository
class SurveyCategoryQuery (
    private val surveyCategoryMapper: SurveyCategoryMapper,
    private val surveyCategoryJpaRepository: SurveyCategoryJpaRepository
) : SurveyCategoryRepository {

    override fun save(surveyCategory: SurveyCategory): SurveyCategory {
        val entity = surveyCategoryMapper.toEntity(surveyCategory)
        return surveyCategoryMapper.toDomain(surveyCategoryJpaRepository.save(entity))
    }

    override fun findById(id: Long): SurveyCategory? {
        return surveyCategoryJpaRepository.findById(id)
            .map { surveyCategoryMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findActive(): List<SurveyCategory> {
        return surveyCategoryJpaRepository.findByIsDeletedFalse()
            .map { surveyCategoryMapper.toDomain(it) }
    }

    override fun existsByParentIdAndIsDeletedFalse(parentId: Long): Boolean {
        return surveyCategoryJpaRepository.existsByParentIdAndIsDeletedFalse(parentId)
    }
    
    override fun findByIdAndIsDeletedFalse(id: Long): SurveyCategory? {
        return surveyCategoryJpaRepository.findByIdAndIsDeletedFalse(id)
            ?.let { surveyCategoryMapper.toDomain(it) }
    }
    
    override fun existsByNameAndParentIdAndIsDeletedFalse(name: String, parentId: Long?, excludeId: Long?): Boolean {
        return surveyCategoryJpaRepository.existsByNameAndParentIdAndIsDeletedFalse(name, parentId, excludeId)
    }
    
    override fun existsBySortOrderAndParentIdAndIsDeletedFalse(sortOrder: Int, parentId: Long?, excludeId: Long?): Boolean {
        return surveyCategoryJpaRepository.existsBySortOrderAndParentIdAndIsDeletedFalse(sortOrder, parentId, excludeId)
    }
    
    override fun countChildrenByParentIdAndIsDeletedFalse(parentId: Long): Long {
        return surveyCategoryJpaRepository.countByParentIdAndIsDeletedFalse(parentId)
    }

    override fun findByNameAndType(name: String, type: SurveyCategoryType): SurveyCategory? {
        return surveyCategoryJpaRepository.findByNameAndTypeAndIsDeletedFalse(name, type)
            ?.let { surveyCategoryMapper.toDomain(it) }
    }
}