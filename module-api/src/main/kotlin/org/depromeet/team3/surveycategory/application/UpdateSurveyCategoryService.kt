package org.depromeet.team3.surveycategory.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.dto.request.UpdateSurveyCategoryRequest
import org.depromeet.team3.surveycategory.exception.SurveyCategoryException
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
            ?: throw SurveyCategoryException(ErrorCode.CATEGORY_NOT_FOUND, mapOf("id" to id))

        // 2. 부모 카테고리 검증 (parentId가 제공된 경우)
        val parentCategory = request.parentId?.let { parentId ->
            val parent = surveyCategoryRepository.findByIdAndIsDeletedFalse(parentId)
                ?: throw SurveyCategoryException(ErrorCode.PARENT_CATEGORY_NOT_FOUND, mapOf("parentId" to parentId))
            
            // 부모 카테고리가 BRANCH 레벨인지 확인
            if (parent.level != SurveyCategoryLevel.BRANCH) {
                throw SurveyCategoryException(ErrorCode.INVALID_REQUEST, mapOf("reason" to "부모 카테고리는 BRANCH 레벨이어야 합니다", "parentLevel" to parent.level))
            }
            
            // 부모-자식 타입 호환성 검증
            if (parent.type != request.type) {
                throw SurveyCategoryException(ErrorCode.INVALID_REQUEST, mapOf("reason" to "부모 카테고리와 자식 카테고리의 타입이 일치하지 않습니다", "parentType" to parent.type, "childType" to request.type))
            }
            
            parent
        }

        // 3. LEAF/BRANCH 규칙 검증
        validateLeafBranchRules(existingCategory, request)

        // 4. 현재 카테고리가 BRANCH에서 LEAF로 변경되는 경우, 자식이 있는지 확인
        if (existingCategory.level == SurveyCategoryLevel.BRANCH &&
            request.level == SurveyCategoryLevel.LEAF) {
            val childCount = surveyCategoryRepository.countChildrenByParentIdAndIsDeletedFalse(id)
            if (childCount > 0) {
                throw SurveyCategoryException(ErrorCode.INVALID_CATEGORY_LEVEL_CHANGE, mapOf("childCount" to childCount))
            }
        }

        // 5. 형제 카테고리 내 이름 중복 검증
        if (surveyCategoryRepository.existsByNameAndParentIdAndIsDeletedFalse(request.name, request.parentId, id)) {
            throw SurveyCategoryException(ErrorCode.DUPLICATE_CATEGORY_NAME, mapOf("name" to request.name, "parentId" to request.parentId))
        }

        // 6. 형제 카테고리 내 순서 중복 검증
        if (surveyCategoryRepository.existsBySortOrderAndParentIdAndIsDeletedFalse(request.sortOrder, request.parentId, id)) {
            throw SurveyCategoryException(ErrorCode.DUPLICATE_CATEGORY_ORDER, mapOf("sortOrder" to request.sortOrder, "parentId" to request.parentId))
        }

        // 7. 업데이트된 카테고리 생성
        val updatedCategory = existingCategory.copy(
            parentId = request.parentId,
            type = request.type,
            level = request.level,
            name = request.name,
            sortOrder = request.sortOrder
        )

        // 8. 저장
        surveyCategoryRepository.save(updatedCategory)
    }

    private fun validateLeafBranchRules(existingCategory: SurveyCategory, request: UpdateSurveyCategoryRequest) {
        // BRANCH 카테고리는 자식을 가질 수 있어야 함
        if (request.level == SurveyCategoryLevel.BRANCH) {
            // BRANCH 카테고리는 자식을 가질 수 있으므로 추가 검증 불필요
        }
        
        // LEAF 카테고리는 자식을 가질 수 없음
        if (request.level == SurveyCategoryLevel.LEAF) {
            // LEAF로 설정하는 경우, 현재 자식이 있는지 확인
            val childCount = surveyCategoryRepository.countChildrenByParentIdAndIsDeletedFalse(existingCategory.id!!)
            if (childCount > 0) {
                throw SurveyCategoryException(ErrorCode.INVALID_CATEGORY_LEVEL_CHANGE, mapOf("childCount" to childCount))
            }
        }
    }
}
