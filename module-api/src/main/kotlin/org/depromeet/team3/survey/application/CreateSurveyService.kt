package org.depromeet.team3.survey.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.enums.SurveyCategoryType
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.meetingattendee.MeetingAttendeeJpaRepository
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.survey.dto.request.SurveyCreateRequest
import org.depromeet.team3.survey.dto.response.SurveyCreateResponse
import org.depromeet.team3.survey.exception.SurveyException
import org.depromeet.team3.survey.Survey
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveyresult.SurveyResult
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateSurveyService(
    private val surveyRepository: SurveyRepository,
    private val surveyResultRepository: SurveyResultRepository,
    private val surveyCategoryRepository: SurveyCategoryRepository,
    private val meetingJpaRepository: MeetingJpaRepository,
    private val meetingAttendeeJpaRepository: MeetingAttendeeJpaRepository
) {
    
    @Transactional
    fun invoke(meetingId: Long, userId: Long, request: SurveyCreateRequest): SurveyCreateResponse {
        // 모임 존재 확인
        if (!meetingJpaRepository.existsById(meetingId)){
           throw SurveyException(ErrorCode.MEETING_NOT_FOUND, mapOf("meetingId" to meetingId))
        }
        
        // 인증된 사용자가 자신의 설문만 제출할 수 있도록 검증
        if (userId != request.participantId) {
            throw SurveyException(ErrorCode.PARTICIPANT_NOT_FOUND, mapOf("participantId" to request.participantId))
        }
        
        // 참가자 존재 확인
        if (!meetingAttendeeJpaRepository.existsByMeetingIdAndUserId(meetingId, request.participantId)) {
            throw SurveyException(ErrorCode.PARTICIPANT_NOT_FOUND, mapOf("participantId" to request.participantId))
        }
        
        // 중복 설문 제출 확인
        if (surveyRepository.existsByMeetingIdAndParticipantId(meetingId, request.participantId)) {
            throw SurveyException(ErrorCode.SURVEY_ALREADY_SUBMITTED, mapOf("participantId" to request.participantId))
        }
        
        // 설문 생성
        val survey = Survey(
            meetingId = meetingId,
            participantId = request.participantId
        )
        val savedSurvey = surveyRepository.save(survey)
        
        // 설문 결과 생성 (선택된 카테고리들을 SurveyResult로 저장)
        val surveyResults = mutableListOf<SurveyResult>()
        
        // 선호 음식 카테고리들 찾아서 결과에 추가
        request.preferredCuisineList.forEach { cuisineName ->
            val category = surveyCategoryRepository.findByNameAndType(cuisineName, SurveyCategoryType.CUISINE)
            if (category != null) {
                surveyResults.add(
                    SurveyResult(
                        surveyId = savedSurvey.id ?: throw IllegalStateException("Survey ID is null"),
                        surveyCategoryId = category.id ?: throw IllegalStateException("Category ID is null")
                    )
                )
            }
        }
        
        // 피해야 하는 재료 카테고리들 찾아서 결과에 추가
        request.avoidIngredientList.forEach { ingredientName ->
            val category = surveyCategoryRepository.findByNameAndType(ingredientName, SurveyCategoryType.AVOID_INGREDIENT)
            if (category != null) {
                surveyResults.add(
                    SurveyResult(
                        surveyId = savedSurvey.id ?: throw IllegalStateException("Survey ID is null"),
                        surveyCategoryId = category.id ?: throw IllegalStateException("Category ID is null")
                    )
                )
            }
        }
        
        // 원하지 않는 메뉴 카테고리들 찾아서 결과에 추가
        request.avoidMenuList.forEach { menuName ->
            val category = surveyCategoryRepository.findByNameAndType(menuName, SurveyCategoryType.AVOID_MENU)
            if (category != null) {
                surveyResults.add(
                    SurveyResult(
                        surveyId = savedSurvey.id ?: throw IllegalStateException("Survey ID is null"),
                        surveyCategoryId = category.id ?: throw IllegalStateException("Category ID is null")
                    )
                )
            }
        }
        
        // 설문 결과 저장
        surveyResultRepository.saveAll(surveyResults)
        
        return SurveyCreateResponse()
    }
}
