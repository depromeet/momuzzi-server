package org.depromeet.team3.survey_category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SurveyCategoryJpaRepository : JpaRepository<SurveyCategoryEntity, Long> {
    
    fun findByIsDeletedFalse(): List<SurveyCategoryEntity>
}