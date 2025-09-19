package org.depromeet.team3.surveycategory.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.config.SecurityTestConfig
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveycategory.SurveyCategoryType
import org.depromeet.team3.surveycategory.application.CreateSurveyCategoryService
import org.depromeet.team3.surveycategory.application.DeleteSurveyCategoryService
import org.depromeet.team3.surveycategory.application.GetSurveyCategoryService
import org.depromeet.team3.surveycategory.application.UpdateSurveyCategoryService
import org.depromeet.team3.surveycategory.controller.SurveyCategoryController
import org.depromeet.team3.surveycategory.dto.request.CreateSurveyCategoryRequest
import org.depromeet.team3.surveycategory.dto.request.UpdateSurveyCategoryRequest
import org.depromeet.team3.surveycategory.dto.response.SurveyCategoryItem
import org.depromeet.team3.surveycategory.dto.response.SurveyCategoryResponse
import org.depromeet.team3.surveycategory.exception.SurveyCategoryException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(SurveyCategoryController::class)
@Import(SecurityTestConfig::class)
@ActiveProfiles("test")
@WithMockUser
@DisplayName("[SURVEY CATEGORY] 설문 카테고리 컨트롤러 테스트")
class SurveyCategoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var createSurveyCategoryService: CreateSurveyCategoryService

    @MockitoBean
    private lateinit var getSurveyCategoryService: GetSurveyCategoryService

    @MockitoBean
    private lateinit var updateSurveyCategoryService: UpdateSurveyCategoryService

    @MockitoBean
    private lateinit var deleteSurveyCategoryService: DeleteSurveyCategoryService

    @Test
    @DisplayName("설문 카테고리 목록을 성공적으로 조회한다")
    fun `설문 카테고리 목록을 성공적으로 조회한다`() {
        // given
        val response = SurveyCategoryResponse(
            preferredCuisineCategoryList = listOf(
                SurveyCategoryItem(
                    level = SurveyCategoryLevel.BRANCH,
                    name = "한식",
                    order = 1,
                    children = emptyList()
                )
            ),
            avoidIngredientCategoryList = emptyList(),
            avoidMenuCategoryList = emptyList()
        )

        `when`(getSurveyCategoryService.invoke()).thenReturn(response)

        // when & then
        mockMvc.perform(get("/api/v1/survey-categories"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.preferredCuisineCategoryList").isArray)
            .andExpect(jsonPath("$.data.preferredCuisineCategoryList[0].name").value("한식"))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    @DisplayName("설문 카테고리를 성공적으로 생성한다")
    fun `설문 카테고리를 성공적으로 생성한다`() {
        // given
        val request = CreateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "한식",
            order = 1
        )

        // Mock 설정 - 실제 서비스 호출을 방지
        doNothing().`when`(createSurveyCategoryService).invoke(any())

        // when & then
        mockMvc.perform(
            post("/api/v1/survey-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    @DisplayName("설문 카테고리를 성공적으로 수정한다")
    fun `설문 카테고리를 성공적으로 수정한다`() {
        // given
        val categoryId = 1L
        val request = UpdateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "전통한식",
            order = 2
        )

        // Mock 설정 - 실제 서비스 호출을 방지
        doNothing().`when`(updateSurveyCategoryService).invoke(any(), any())

        // when & then
        mockMvc.perform(
            put("/api/v1/survey-categories/$categoryId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    @DisplayName("설문 카테고리를 성공적으로 삭제한다")
    fun `설문 카테고리를 성공적으로 삭제한다`() {
        // given
        val categoryId = 1L

        // Mock 설정 - 실제 서비스 호출을 방지
        doNothing().`when`(deleteSurveyCategoryService).invoke(any())

        // when & then
        mockMvc.perform(delete("/api/v1/survey-categories/$categoryId")
            .with(csrf()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 수정 시 404 에러가 발생한다")
    fun `존재하지 않는 카테고리 수정 시 404 에러가 발생한다`() {
        // given
        val categoryId = 999L
        val request = UpdateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "전통한식",
            order = 2
        )

        // Mock 설정 - 예외 발생 시뮬레이션
        doThrow(SurveyCategoryException(ErrorCode.CATEGORY_NOT_FOUND, mapOf("id" to categoryId)))
            .`when`(updateSurveyCategoryService).invoke(any(), any())

        // when & then
        mockMvc.perform(
            put("/api/v1/survey-categories/$categoryId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.error.code").value("C4041"))
    }

    @Test
    @DisplayName("하위 카테고리가 있는 카테고리 삭제 시 409 에러가 발생한다")
    fun `하위 카테고리가 있는 카테고리 삭제 시 409 에러가 발생한다`() {
        // given
        val categoryId = 1L

        doThrow(SurveyCategoryException(ErrorCode.CATEGORY_HAS_CHILDREN, mapOf("categoryId" to categoryId)))
            .`when`(deleteSurveyCategoryService).invoke(any())

        // when & then
        mockMvc.perform(delete("/api/v1/survey-categories/$categoryId")
            .with(csrf()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.error.code").value("C4092"))
    }

    @Test
    @DisplayName("잘못된 요청 데이터로 카테고리 생성 시 400 에러가 발생한다")
    fun `잘못된 요청 데이터로 카테고리 생성 시 400 에러가 발생한다`() {
        // given
        val invalidRequest = CreateSurveyCategoryRequest(
            parentId = null,
            type = SurveyCategoryType.CUISINE,
            level = SurveyCategoryLevel.BRANCH,
            name = "", // 빈 문자열 - 유효성 검사 실패
            order = 1
        )

        // when & then
        mockMvc.perform(
            post("/api/v1/survey-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.error.code").value("C002"))
    }
}
