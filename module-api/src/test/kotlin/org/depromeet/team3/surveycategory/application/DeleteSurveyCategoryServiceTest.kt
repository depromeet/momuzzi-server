package org.depromeet.team3.surveycategory.application

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.exception.SurveyCategoryException
import org.depromeet.team3.survey.util.SurveyTestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("설문 카테고리 삭제 서비스 테스트")
class DeleteSurveyCategoryServiceTest {

    @Mock
    private lateinit var surveyCategoryRepository: SurveyCategoryRepository

    private lateinit var deleteSurveyCategoryService: DeleteSurveyCategoryService

    @BeforeEach
    fun setUp() {
        deleteSurveyCategoryService = DeleteSurveyCategoryService(surveyCategoryRepository)
    }

    @Test
    @DisplayName("하위 카테고리가 없는 카테고리를 성공적으로 삭제한다")
    fun `하위 카테고리가 없는 카테고리를 성공적으로 삭제한다`() {
        // given
        val categoryId = 1L
        val categoryToDelete = SurveyTestDataFactory.createSurveyCategory(
            id = categoryId,
            level = SurveyCategoryLevel.LEAF,
            name = "김치찌개",
            sortOrder = 1
        )

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(categoryToDelete)
        `when`(surveyCategoryRepository.existsByParentIdAndIsDeletedFalse(categoryId)).thenReturn(false)

        // when
        deleteSurveyCategoryService(categoryId)

        // then
        verify(surveyCategoryRepository).findById(categoryId)
        verify(surveyCategoryRepository).existsByParentIdAndIsDeletedFalse(categoryId)
        verify(surveyCategoryRepository).save(
            categoryToDelete.copy(isDeleted = true)
        )
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 삭제 시 예외가 발생한다")
    fun `존재하지 않는 카테고리 삭제 시 예외가 발생한다`() {
        // given
        val categoryId = 999L

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(null)

        // when & then
        assertThatThrownBy { deleteSurveyCategoryService(categoryId) }
            .isInstanceOf(SurveyCategoryException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND)
    }

    @Test
    @DisplayName("하위 카테고리가 있는 카테고리 삭제 시 예외가 발생한다")
    fun `하위 카테고리가 있는 카테고리 삭제 시 예외가 발생한다`() {
        // given
        val categoryId = 1L
        val categoryToDelete = SurveyTestDataFactory.createSurveyCategory(
            id = categoryId,
            level = SurveyCategoryLevel.BRANCH,
            name = "한식",
            sortOrder = 1
        )

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(categoryToDelete)
        `when`(surveyCategoryRepository.existsByParentIdAndIsDeletedFalse(categoryId)).thenReturn(true)

        // when & then
        assertThatThrownBy { deleteSurveyCategoryService(categoryId) }
            .isInstanceOf(SurveyCategoryException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_HAS_CHILDREN)
    }

    @Test
    @DisplayName("이미 삭제된 카테고리도 정상적으로 처리된다")
    fun `이미 삭제된 카테고리도 정상적으로 처리된다`() {
        // given
        val categoryId = 1L
        val alreadyDeletedCategory = SurveyTestDataFactory.createSurveyCategory(
            id = categoryId,
            level = SurveyCategoryLevel.LEAF,
            name = "김치찌개",
            sortOrder = 1,
            isDeleted = true
        )

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(alreadyDeletedCategory)
        `when`(surveyCategoryRepository.existsByParentIdAndIsDeletedFalse(categoryId)).thenReturn(false)

        // when
        deleteSurveyCategoryService(categoryId)

        // then
        verify(surveyCategoryRepository).findById(categoryId)
        verify(surveyCategoryRepository).existsByParentIdAndIsDeletedFalse(categoryId)
        verify(surveyCategoryRepository).save(
            alreadyDeletedCategory.copy(isDeleted = true)
        )
    }
}
