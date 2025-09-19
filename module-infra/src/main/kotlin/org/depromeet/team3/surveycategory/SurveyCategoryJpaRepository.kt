package org.depromeet.team3.survey_category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SurveyCategoryJpaRepository : JpaRepository<SurveyCategoryEntity, Long> {
    
    fun findByIsDeletedFalse(): List<SurveyCategoryEntity>

    fun existsByParentIdAndIsDeletedFalse(parentId: Long): Boolean
    
    fun findByIdAndIsDeletedFalse(id: Long): SurveyCategoryEntity?
    
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END 
        FROM SurveyCategoryEntity c 
        WHERE c.name = :name 
        AND (:parentId IS NULL AND c.parent IS NULL OR c.parent.id = :parentId)
        AND c.isDeleted = false
        AND (:excludeId IS NULL OR c.id != :excludeId)
    """)
    fun existsByNameAndParentIdAndIsDeletedFalse(
        @Param("name") name: String, 
        @Param("parentId") parentId: Long?, 
        @Param("excludeId") excludeId: Long?
    ): Boolean
    
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END 
        FROM SurveyCategoryEntity c 
        WHERE c.order = :order 
        AND (:parentId IS NULL AND c.parent IS NULL OR c.parent.id = :parentId)
        AND c.isDeleted = false
        AND (:excludeId IS NULL OR c.id != :excludeId)
    """)
    fun existsByOrderAndParentIdAndIsDeletedFalse(
        @Param("order") order: Int, 
        @Param("parentId") parentId: Long?, 
        @Param("excludeId") excludeId: Long?
    ): Boolean
    
    fun countByParentIdAndIsDeletedFalse(parentId: Long): Long
}