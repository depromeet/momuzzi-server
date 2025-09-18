package org.depromeet.team3.survey_category.application

import org.depromeet.team3.survey_category.SurveyCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteSurveyCategoryService(
    private val surveyCategoryRepository: SurveyCategoryRepository
) {

    @Transactional
    operator fun invoke(id: Long): Unit {
        // 1. 삭제할 카테고리 조회
        val categoryToDelete = surveyCategoryRepository.findById(id)
            ?: throw IllegalArgumentException("카테고리를 찾을 수 없습니다. ID: $id")

        // 2. 하위 카테고리 존재 여부 확인
        val hasChildren = surveyCategoryRepository.existsByParentIdAndIsDeletedFalse(id)
        
        if (hasChildren) {
            throw IllegalStateException(
                "하위 카테고리가 존재하는 카테고리는 삭제할 수 없습니다. " +
                "먼저 하위 카테고리를 삭제해주세요. 카테고리명: ${categoryToDelete.name}"
            )
        }

        // 3. Soft Delete 처리
        val deletedCategory = categoryToDelete.copy(isDeleted = true)
        surveyCategoryRepository.save(deletedCategory)
    }
}
