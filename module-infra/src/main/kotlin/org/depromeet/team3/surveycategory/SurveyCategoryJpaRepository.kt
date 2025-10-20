package org.depromeet.team3.surveycategory

import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SurveyCategoryJpaRepository : JpaRepository<SurveyCategoryEntity, Long> {
    
    fun findByIsDeletedFalse(): List<SurveyCategoryEntity>

    fun existsByParentIdAndIsDeletedFalse(parentId: Long): Boolean
    
    fun findByIdAndIsDeletedFalse(id: Long): SurveyCategoryEntity?
    
    fun existsBySortOrderAndParentIdAndIsDeletedFalseAndIdNot(
        sortOrder: Int, 
        parentId: Long?, 
        excludeId: Long?
    ): Boolean
    
    fun countByParentIdAndIsDeletedFalse(parentId: Long): Long
    
    fun findByNameAndIsDeletedFalse(name: String): SurveyCategoryEntity?
}