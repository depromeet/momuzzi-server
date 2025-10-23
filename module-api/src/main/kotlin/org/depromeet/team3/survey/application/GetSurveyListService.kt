package org.depromeet.team3.survey.application

import org.depromeet.team3.common.exception.ErrorCode
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
        println("DEBUG: Found ${surveys.size} surveys for meetingId=$meetingId")
        
        // 전체 참가자 수 조회
        val totalParticipants = meetingAttendeeRepository.countByMeetingId(meetingId)
        println("DEBUG: Total participants: $totalParticipants")
        
        // 설문 참여율 계산 (참여자 수 / 전체 참가자 수) * 100
        val participationRate = if (totalParticipants > 0) {
            (surveys.size.toDouble() / totalParticipants) * 100.0
        } else {
            0.0
        }
        
        // 설문 완료 여부 (모든 참가자가 설문을 완료했는지)
        val isCompleted = surveys.size == totalParticipants
        
        println("DEBUG: Participation rate: $participationRate, Is completed: $isCompleted")
        
        // 각 설문의 결과 정보와 함께 응답 생성
        val surveyItems = surveys.map { survey ->
            println("DEBUG: Processing survey id=${survey.id}, participantId=${survey.participantId}")
            
            val results = surveyResultRepository.findBySurveyId(survey.id!!)
            
            // 설문 결과에서 카테고리 목록 생성
            val selectedCategoryList = results.map { it.surveyCategoryId }
            
            // 참가자 정보 조회
            val participant = meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, survey.participantId)
            println("DEBUG: Participant found: $participant for meetingId=$meetingId, participantId=${survey.participantId}")
            
            if (participant == null) {
                throw SurveyException(ErrorCode.PARTICIPANT_NOT_FOUND, mapOf("participantId" to survey.participantId))
            }
            
            SurveyItemResponse(
                participantId = survey.participantId,
                nickname = participant.attendeeNickname ?: "알 수 없음",
                selectedCategoryList = selectedCategoryList
            )
        }
        
        return SurveyListResponse(
            surveys = surveyItems,
            participationRate = participationRate,
            isCompleted = isCompleted
        )
    }
}
