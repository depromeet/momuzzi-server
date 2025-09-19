package org.depromeet.team3.survey_category.application

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.depromeet.team3.common.exception.DpmException
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.survey_category.SurveyCategory
import org.depromeet.team3.survey_category.SurveyCategoryLevel
import org.depromeet.team3.survey_category.SurveyCategoryRepository
import org.depromeet.team3.survey_category.SurveyCategoryType
import org.depromeet.team3.survey_category.dto.request.UpdateSurveyCategoryRequest
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
            order = 1,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        val updateRequest = UpdateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "전통한식",
            order = 2
        )

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(existingCategory)

        // when
        updateSurveyCategoryService(categoryId, updateRequest)

        // then
        verify(surveyCategoryRepository).findById(categoryId)
        verify(surveyCategoryRepository).save(
            existingCategory.copy(
                name = "전통한식",
                order = 2
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
            order = 2
        )

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(null)

        // when & then
        assertThatThrownBy { updateSurveyCategoryService(categoryId, updateRequest) }
            .isInstanceOf(DpmException::class.java)
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
            order = 1,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )

        val updateRequest = UpdateSurveyCategoryRequest(
            parentId = 2L,
            type = SurveyCategoryType.AVOID_INGREDIENT,
            level = SurveyCategoryLevel.LEAF,
            name = "피해야할 재료",
            order = 5
        )

        `when`(surveyCategoryRepository.findById(categoryId)).thenReturn(existingCategory)

        // when
        updateSurveyCategoryService(categoryId, updateRequest)

        // then
        verify(surveyCategoryRepository).save(
            existingCategory.copy(
                parentId = 2L,
                type = SurveyCategoryType.AVOID_INGREDIENT,
                level = SurveyCategoryLevel.LEAF,
                name = "피해야할 재료",
                order = 5
            )
        )
    }
}
