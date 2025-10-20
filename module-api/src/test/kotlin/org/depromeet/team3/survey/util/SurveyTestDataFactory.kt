package org.depromeet.team3.survey.util

import org.depromeet.team3.survey.Survey
import org.depromeet.team3.survey.dto.request.SurveyCreateRequest
import org.depromeet.team3.survey.dto.response.SurveyCreateResponse
import org.depromeet.team3.survey.dto.response.SurveyItemResponse
import org.depromeet.team3.survey.dto.response.SurveyListResponse
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveyresult.SurveyResult
import org.depromeet.team3.surveyresult.util.SurveyResultTestDataFactory
import java.time.LocalDateTime

/**
 * Survey 관련 테스트 데이터 팩토리 클래스
 */
object SurveyTestDataFactory {

    // ========== SurveyCreateRequest 생성 ==========
    
    fun createSurveyCreateRequest(
        participantId: Long = 1L,
        nickname: String = "테스트참가자",
        selectedCategoryList: List<Long> = listOf(1L, 3L)
    ): SurveyCreateRequest {
        return SurveyCreateRequest(
            participantId = participantId,
            nickname = nickname,
            selectedCategoryList = selectedCategoryList
        )
    }

    fun createMinimalSurveyCreateRequest(
        participantId: Long = 1L,
        nickname: String = "테스트참가자"
    ): SurveyCreateRequest {
        return SurveyCreateRequest(
            participantId = participantId,
            nickname = nickname,
            selectedCategoryList = listOf(1L)
        )
    }

    fun createEmptySurveyCreateRequest(
        participantId: Long = 1L,
        nickname: String = "테스트참가자"
    ): SurveyCreateRequest {
        return SurveyCreateRequest(
            participantId = participantId,
            nickname = nickname,
            selectedCategoryList = emptyList()
        )
    }

    // ========== SurveyCategory 생성 ==========
    
    fun createSurveyCategory(
        id: Long = 1L,
        parentId: Long? = null,
        level: SurveyCategoryLevel = SurveyCategoryLevel.LEAF,
        name: String = "한식",
        sortOrder: Int = 1,
        isDeleted: Boolean = false,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime? = null
    ): SurveyCategory {
        return SurveyCategory(
            id = id,
            parentId = parentId,
            level = level,
            name = name,
            sortOrder = sortOrder,
            isDeleted = isDeleted,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // ========== Survey 엔티티 생성 ==========
    
    fun createSurvey(
        id: Long = 1L,
        meetingId: Long = 1L,
        participantId: Long = 1L
    ): Survey {
        return Survey(
            id = id,
            meetingId = meetingId,
            participantId = participantId
        )
    }

    // ========== SurveyResult 생성 ==========
    
    fun createSurveyResult(
        id: Long = 1L,
        surveyId: Long = 1L,
        surveyCategoryId: Long = 1L
    ): SurveyResult {
        return SurveyResultTestDataFactory.createSurveyResult(
            id = id,
            surveyId = surveyId,
            surveyCategoryId = surveyCategoryId
        )
    }


    // ========== Response DTO 생성 ==========
    
    fun createSurveyCreateResponse(): SurveyCreateResponse {
        return SurveyCreateResponse()
    }

    fun createSurveyItemResponse(
        participantId: Long = 1L,
        nickname: String = "참가자1",
        selectedCategoryList: List<Long> = listOf(1L, 3L)
    ): SurveyItemResponse {
        return SurveyItemResponse(
            participantId = participantId,
            nickname = nickname,
            selectedCategoryList = selectedCategoryList
        )
    }

    fun createSurveyListResponse(
        surveys: List<SurveyItemResponse> = listOf(
            createSurveyItemResponse(),
            createSurveyItemResponse(
                participantId = 2L,
                nickname = "참가자2",
                selectedCategoryList = listOf(2L)
            )
        )
    ): SurveyListResponse {
        return SurveyListResponse(surveys = surveys)
    }

    // ========== 복합 데이터 생성 ==========
    
    fun createSurveyTestScenario(
        meetingId: Long = 1L,
        participantId: Long = 1L
    ): SurveyTestScenario {
        return SurveyTestScenario(
            meetingId = meetingId,
            participantId = participantId,
            request = createSurveyCreateRequest(participantId = participantId),
            survey = createSurvey(meetingId = meetingId, participantId = participantId),
            cuisineCategory = createSurveyCategory(
                id = 1L,
                name = "한식"
            ),
            japaneseCuisineCategory = createSurveyCategory(
                id = 3L,
                name = "일식"
            ),
            ingredientCategory = createSurveyCategory(
                id = 2L,
                name = "글루텐"
            ),
            response = createSurveyCreateResponse()
        )
    }

    data class SurveyTestScenario(
        val meetingId: Long,
        val participantId: Long,
        val request: SurveyCreateRequest,
        val survey: Survey,
        val cuisineCategory: SurveyCategory,
        val japaneseCuisineCategory: SurveyCategory,
        val ingredientCategory: SurveyCategory,
        val response: SurveyCreateResponse
    )
}
