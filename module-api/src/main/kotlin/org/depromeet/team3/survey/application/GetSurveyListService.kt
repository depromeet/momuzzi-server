package org.depromeet.team3.survey.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.survey.dto.GetRespondents
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.depromeet.team3.survey.dto.response.SurveyItemResponse
import org.depromeet.team3.survey.dto.response.SurveyListResponse
import org.depromeet.team3.survey.exception.SurveyException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetSurveyListService(
    private val surveyRepository: SurveyRepository,
    private val surveyResultRepository: SurveyResultRepository,
    private val meetingJpaRepository: MeetingJpaRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository
) {
    private val logger = LoggerFactory.getLogger(GetSurveyListService::class.java)

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
        logger.debug("Found {} surveys for meetingId={}", surveys.size, meetingId)

        // 전체 참가자 수 조회
        val totalParticipants = meetingAttendeeRepository.countByMeetingId(meetingId)
        logger.debug("Total participants: {} for meetingId={}", totalParticipants, meetingId)

        // 설문 참여율 계산 (참여자 수 / 전체 참가자 수) * 100
        val participationRate = if (totalParticipants > 0) {
            (surveys.size.toDouble() / totalParticipants) * 100.0
        } else {
            0.0
        }

        // 설문 완료 여부 (모든 참가자가 설문을 완료했는지)
        val isCompleted = surveys.size == totalParticipants

        logger.debug("Participation rate: {}, Is completed: {} for meetingId={}", participationRate, isCompleted, meetingId)

        // 모든 참가자를 한 번에 조회 (N+1 문제 해결)
        val attendeeList = meetingAttendeeRepository.findByMeetingId(meetingId)
        val attendeeMap = attendeeList.associateBy { it.userId }

        // 각 설문의 결과 정보와 함께 응답 생성
        val surveyItems = surveys.map { survey ->
            logger.debug("Processing survey id={}, participantId={} for meetingId={}", survey.id, survey.participantId, meetingId)

            val results = surveyResultRepository.findBySurveyId(survey.id!!)

            // 설문 결과에서 카테고리 목록 생성
            val selectedCategoryList = results.map { it.surveyCategoryId }

            // Map에서 참가자 정보 조회
            val participant = attendeeMap[survey.participantId]
            logger.debug("Participant found: {} for meetingId={}, participantId={}", participant, meetingId, survey.participantId)

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

    @Transactional(readOnly = true)
    fun getRespondents(meetingId: Long): List<GetRespondents> {
        // 모든 참가자를 한 번에 조회 (N+1 문제 해결)
        val attendeeList = meetingAttendeeRepository.findByMeetingId(meetingId)
        val attendeeMap = attendeeList.associateBy { it.userId }
        
        // 설문 작성자 ID 목록 조회
        val participantIdList = surveyRepository.findByMeetingId(meetingId)
            .map { it.participantId }
        
        return participantIdList
            .mapNotNull { id -> attendeeMap[id] }
            .map { attendee -> attendee.toGetRespondents() }
    }

    private fun MeetingAttendee.toGetRespondents(): GetRespondents {
        return GetRespondents(
            userId = this.userId,
            attendeeNickname = this.attendeeNickname ?: "",
            color = this.muzziColor.name
        )
    }
}
