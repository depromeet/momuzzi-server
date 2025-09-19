package org.depromeet.team3.surveycategory.application

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.SurveyCategoryType
import org.depromeet.team3.surveycategory.dto.request.UpdateSurveyCategoryRequest
import org.depromeet.team3.surveycategory.exception.SurveyCategoryException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("설문 카테고리 수정 서비스 테스트")
class UpdateSurveyCategoryServiceTest {

    @Mock
    private lateinit var surveyCategoryRepository: SurveyCategoryRepository

    private lateinit var updateSurveyCategoryService: UpdateSurveyCategoryService

    @BeforeEach
    fun setUp() {
        updateSurveyCategoryService = UpdateSurveyCategoryService(surveyCategoryRepository)
    }

    @Test
    @DisplayName("존재하는 카테고리를 성공적으로 수정한다")
    fun `존재하는 카테고리를 성공적으로 수정한다`() {
        // given
        val categoryId = 1L
        val existingCategory = SurveyCategory(
            id = categoryId,
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "한식",
            sortOrder = 1,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        val updateRequest = UpdateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "전통한식",
            sortOrder = 2
        )

        `when`(surveyCategoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(existingCategory)

        // when
        updateSurveyCategoryService(categoryId, updateRequest)

        // then
        verify(surveyCategoryRepository).findByIdAndIsDeletedFalse(categoryId)
        verify(surveyCategoryRepository).save(
            existingCategory.copy(
                name = "전통한식",
                sortOrder = 2
            )
        )
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 수정 시 예외가 발생한다")
    fun `존재하지 않는 카테고리 수정 시 예외가 발생한다`() {
        // given
        val categoryId = 999L
        val updateRequest = UpdateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "전통한식",
            sortOrder = 2
        )

        `when`(surveyCategoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(null)

        // when & then
        assertThatThrownBy { updateSurveyCategoryService(categoryId, updateRequest) }
            .isInstanceOf(SurveyCategoryException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND)
    }

    @Test
    @DisplayName("카테고리의 모든 필드를 수정할 수 있다")
    fun `카테고리의 모든 필드를 수정할 수 있다`() {
        // given
        val categoryId = 1L
        val existingCategory = SurveyCategory(
            id = categoryId,
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "한식",
            sortOrder = 1,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        val updateRequest = UpdateSurveyCategoryRequest(
            parentId = 2L,
            type = SurveyCategoryType.AVOID_INGREDIENT,
            level = SurveyCategoryLevel.LEAF,
            name = "피해야할 재료",
            sortOrder = 5
        )

        val parentCategory = SurveyCategory(
            id = 2L,
            parentId = null,
            type = SurveyCategoryType.AVOID_INGREDIENT,
            level = SurveyCategoryLevel.BRANCH,
            name = "부모 카테고리",
            sortOrder = 1,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        `when`(surveyCategoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(existingCategory)
        `when`(surveyCategoryRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(parentCategory)
        `when`(surveyCategoryRepository.countChildrenByParentIdAndIsDeletedFalse(categoryId)).thenReturn(0L)
        `when`(surveyCategoryRepository.existsByNameAndParentIdAndIsDeletedFalse("피해야할 재료", 2L, categoryId)).thenReturn(false)
        `when`(surveyCategoryRepository.existsBySortOrderAndParentIdAndIsDeletedFalse(5, 2L, categoryId)).thenReturn(false)

        // when
        updateSurveyCategoryService(categoryId, updateRequest)

        // then
        verify(surveyCategoryRepository).save(
            existingCategory.copy(
                parentId = 2L,
                type = SurveyCategoryType.AVOID_INGREDIENT,
                level = SurveyCategoryLevel.LEAF,
                name = "피해야할 재료",
                sortOrder = 5
            )
        )
    }
}
