package org.depromeet.team3.surveycategory

interface SurveyCategoryRepository {

    fun save(surveyCategory: SurveyCategory): SurveyCategory

    fun findById(id: Long): SurveyCategory?

    fun findActive(): List<SurveyCategory>

    fun existsByParentIdAndIsDeletedFalse(parentId: Long): Boolean
    
    fun findByIdAndIsDeletedFalse(id: Long): SurveyCategory?
    
    fun existsByNameAndParentIdAndIsDeletedFalse(name: String, parentId: Long?, excludeId: Long? = null): Boolean
    
    fun existsBySortOrderAndParentIdAndIsDeletedFalse(sortOrder: Int, parentId: Long?, excludeId: Long? = null): Boolean
    
    fun countChildrenByParentIdAndIsDeletedFalse(parentId: Long): Long
    
    fun findByName(name: String): SurveyCategory?
}