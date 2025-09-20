package org.depromeet.team3.survey.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.enums.SurveyCategoryType
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.survey.dto.response.SurveyItemResponse
import org.depromeet.team3.survey.dto.response.SurveyListResponse
import org.depromeet.team3.survey.exception.SurveyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetSurveyListService(
    private val surveyRepository: SurveyRepository,
    private val surveyResultRepository: SurveyResultRepository,
    private val surveyCategoryRepository: SurveyCategoryRepository,
    private val meetingJpaRepository: MeetingJpaRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {
    
    @Transactional(readOnly = true)
    fun invoke(meetingId: Long, userId: Long): SurveyListResponse {
        // 모임 존재 확인
        if (!meetingJpaRepository.existsById(meetingId)) {
            throw SurveyException(ErrorCode.MEETING_NOT_FOUND, mapOf("meetingId" to meetingId))
        }
        
        // 사용자가 해당 모임의 참가자인지 확인
        if (!meetingAttendeeRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw SurveyException(ErrorCode.PARTICIPANT_NOT_FOUND, mapOf("userId" to userId))
        }
        
        // 모임의 모든 설문 조회
        val surveys = surveyRepository.findByMeetingId(meetingId)
        
        // 각 설문의 결과 정보와 함께 응답 생성
        val surveyItems = surveys.map { survey ->
            val results = surveyResultRepository.findBySurveyId(survey.id!!)
            
            // 설문 결과에서 카테고리 정보 조회
            val preferredCuisineList = mutableListOf<String>()
            val avoidIngredientList = mutableListOf<String>()
            val avoidMenuList = mutableListOf<String>()
            
            results.forEach { result ->
                val category = surveyCategoryRepository.findById(result.surveyCategoryId)
                if (category != null) {
                    when (category.type) {
                        SurveyCategoryType.CUISINE -> preferredCuisineList.add(category.name)
                        SurveyCategoryType.AVOID_INGREDIENT -> avoidIngredientList.add(category.name)
                        SurveyCategoryType.AVOID_MENU -> avoidMenuList.add(category.name)
                    }
                }
            }
            
            // 참가자 정보 조회
            val participant = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, survey.participantId)
                ?: throw SurveyException(ErrorCode.PARTICIPANT_NOT_FOUND, mapOf("participantId" to survey.participantId))
            
            SurveyItemResponse(
                participantId = survey.participantId,
                nickname = participant.attendeeNickname,
                preferredCuisineList = preferredCuisineList,
                avoidIngredientList = avoidIngredientList,
                avoidMenuList = avoidMenuList
            )
        }
        
        return SurveyListResponse(surveys = surveyItems)
    }
}
