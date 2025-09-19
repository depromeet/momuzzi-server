package org.depromeet.team3.survey_category.application

import org.depromeet.team3.survey_category.SurveyCategory
import org.depromeet.team3.survey_category.SurveyCategoryRepository
import org.depromeet.team3.survey_category.dto.request.UpdateSurveyCategoryRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateSurveyCategoryService(
    private val surveyCategoryRepository: SurveyCategoryRepository
) {

    @Transactional
    operator fun invoke(id: Long, request: UpdateSurveyCategoryRequest): Unit {
        // 1. 기존 카테고리 조회 및 삭제 상태 검증
        val existingCategory = surveyCategoryRepository.findByIdAndIsDeletedFalse(id)
            ?: throw IllegalArgumentException("카테고리를 찾을 수 없거나 삭제된 카테고리입니다. ID: $id")

        // 2. 부모 카테고리 검증 (parentId가 제공된 경우)
        val parentCategory = request.parentId?.let { parentId ->
            val parent = surveyCategoryRepository.findByIdAndIsDeletedFalse(parentId)
                ?: throw IllegalArgumentException("부모 카테고리를 찾을 수 없거나 삭제된 카테고리입니다. 부모 ID: $parentId")
            
            // 부모 카테고리가 BRANCH 레벨인지 확인
            if (parent.level != org.depromeet.team3.survey_category.SurveyCategoryLevel.BRANCH) {
                throw IllegalArgumentException("부모 카테고리는 BRANCH 레벨이어야 합니다. 부모 카테고리 레벨: ${parent.level}")
            }
            
            // 부모-자식 타입 호환성 검증
            if (parent.type != request.type) {
                throw IllegalArgumentException("부모 카테고리와 자식 카테고리의 타입이 일치하지 않습니다. 부모 타입: ${parent.type}, 자식 타입: ${request.type}")
            }
            
            parent
        }

        // 3. LEAF/BRANCH 규칙 검증
        validateLeafBranchRules(existingCategory, request)

        // 4. 현재 카테고리가 BRANCH에서 LEAF로 변경되는 경우, 자식이 있는지 확인
        if (existingCategory.level == org.depromeet.team3.survey_category.SurveyCategoryLevel.BRANCH && 
            request.level == org.depromeet.team3.survey_category.SurveyCategoryLevel.LEAF) {
            val childCount = surveyCategoryRepository.countChildrenByParentIdAndIsDeletedFalse(id)
            if (childCount > 0) {
                throw IllegalArgumentException("자식 카테고리가 있는 BRANCH 카테고리를 LEAF로 변경할 수 없습니다. 자식 카테고리 수: $childCount")
            }
        }

        // 5. 형제 카테고리 내 이름 중복 검증
        if (surveyCategoryRepository.existsByNameAndParentIdAndIsDeletedFalse(request.name, request.parentId, id)) {
            throw IllegalArgumentException("같은 부모 하위에 동일한 이름의 카테고리가 이미 존재합니다. 이름: ${request.name}")
        }

        // 6. 형제 카테고리 내 순서 중복 검증
        if (surveyCategoryRepository.existsByOrderAndParentIdAndIsDeletedFalse(request.order, request.parentId, id)) {
            throw IllegalArgumentException("같은 부모 하위에 동일한 순서의 카테고리가 이미 존재합니다. 순서: ${request.order}")
        }

        // 7. 업데이트된 카테고리 생성
        val updatedCategory = existingCategory.copy(
            parentId = request.parentId,
            type = request.type,
            level = request.level,
            name = request.name,
            order = request.order
        )

        // 8. 저장
        surveyCategoryRepository.save(updatedCategory)
    }

    private fun validateLeafBranchRules(existingCategory: SurveyCategory, request: UpdateSurveyCategoryRequest) {
        // BRANCH 카테고리는 자식을 가질 수 있어야 함
        if (request.level == org.depromeet.team3.survey_category.SurveyCategoryLevel.BRANCH) {
            // BRANCH 카테고리는 자식을 가질 수 있으므로 추가 검증 불필요
        }
        
        // LEAF 카테고리는 자식을 가질 수 없음
        if (request.level == org.depromeet.team3.survey_category.SurveyCategoryLevel.LEAF) {
            // LEAF로 설정하는 경우, 현재 자식이 있는지 확인
            val childCount = surveyCategoryRepository.countChildrenByParentIdAndIsDeletedFalse(existingCategory.id!!)
            if (childCount > 0) {
                throw IllegalArgumentException("자식 카테고리가 있는 카테고리를 LEAF로 변경할 수 없습니다. 자식 카테고리 수: $childCount")
            }
        }
    }
}
