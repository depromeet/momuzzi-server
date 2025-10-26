package org.depromeet.team3.meeting.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.response.MeetingDetailResponse
import org.depromeet.team3.meeting.dto.response.MeetingInfoResponse
import org.depromeet.team3.meeting.dto.response.MeetingParticipantInfo
import org.depromeet.team3.meeting.dto.response.ParticipantSelectedCategory
import org.depromeet.team3.meeting.dto.response.SelectedLeafCategory
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.station.StationRepository
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.requireNotNull

@Service
class GetMeetingDetailService(
    private val meetingRepository: MeetingRepository,
    private val stationRepository: StationRepository,
    private val meetingAttendeeRepository: MeetingAttendeeRepository,
    private val surveyRepository: SurveyRepository,
    private val surveyResultRepository: SurveyResultRepository,
    private val surveyCategoryRepository: SurveyCategoryRepository
) {

    @Transactional(readOnly = true)
    fun invoke(meetingId: Long, userId: Long): MeetingDetailResponse {
        // 모임 조회
        val meeting = meetingRepository.findById(meetingId)
            ?: throw MeetingException(ErrorCode.MEETING_NOT_FOUND, mapOf("meetingId" to meetingId))

        // 역 정보 조회
        val station = stationRepository.findById(meeting.stationId)
        val stationName = station?.name ?: ""

        // 필수 필드 검증
        val validatedMeetingId = requireNotNull(meeting.id) { "모임 ID는 필수입니다" }
        val endAt = requireNotNull(meeting.endAt) { "모임 종료 시간은 필수입니다" }
        val createdAt = requireNotNull(meeting.createdAt) { "모임 생성 시간은 필수입니다" }

        // MeetingInfoResponse 생성
        val meetingInfo = MeetingInfoResponse(
            id = validatedMeetingId,
            title = meeting.name,
            hostUserId = meeting.hostUserId,
            totalParticipantCnt = meeting.attendeeCount,
            isClosed = meeting.isClosed,
            stationName = stationName,
            endAt = endAt,
            createdAt = createdAt,
            updatedAt = meeting.updatedAt
        )

        // 참가자 목록 조회
        val attendeeList = meetingAttendeeRepository.findByMeetingId(meetingId)
        
        // 모든 설문을 한 번에 조회 (N+1 문제 해결)
        val surveyList = surveyRepository.findByMeetingId(meetingId)
        val surveyMap = surveyList.associateBy { it.participantId }
        
        // 설문이 있는 참가자만 participantList에 포함
        val participantList = attendeeList
            .mapNotNull { attendee ->
                // Map에서 참가자의 설문 조회
                val survey = surveyMap[attendee.userId]
                
                // 설문이 없는 경우 null 반환하여 제외
                survey ?: return@mapNotNull null
                
                // 설문이 있는 경우 선택한 카테고리 목록 생성
                val surveyId = requireNotNull(survey.id) { "설문 ID는 필수입니다" }
                val selectedCategoryList = buildParticipantSelectedCategories(surveyId)

                MeetingParticipantInfo(
                    userId = attendee.userId,
                    nickname = attendee.attendeeNickname ?: "알 수 없음",
                    profileColor = attendee.muzziColor.name.lowercase(),
                    selectedCategories = selectedCategoryList
                )
            }

        return MeetingDetailResponse(
            currentUserId = userId,
            meetingInfo = meetingInfo,
            participantList = participantList
        )
    }

    private fun buildParticipantSelectedCategories(surveyId: Long): List<ParticipantSelectedCategory> {
        // 설문 결과 조회
        val surveyResults = surveyResultRepository.findBySurveyId(surveyId)
        if (surveyResults.isEmpty()) {
            return emptyList()
        }

        // 카테고리 ID 목록 조회
        val categoryIdList = surveyResults.map { it.surveyCategoryId }
        val categoryList = surveyCategoryRepository.findAllById(categoryIdList)

        // BRANCH 카테고리와 LEAF 카테고리 분리 (null id 제외)
        val branchCategoryList = categoryList.filter { it.level == SurveyCategoryLevel.BRANCH && it.id != null }
        val leafCategoryList = categoryList.filter { it.level == SurveyCategoryLevel.LEAF && it.id != null }

        // BRANCH 카테고리별로 해당하는 LEAF 카테고리들을 그룹화
        return branchCategoryList.mapNotNull { branchCategory ->
            val branchId = branchCategory.id ?: return@mapNotNull null
            
            val leafCategoriesForBranch = leafCategoryList
                .filter { it.parentId == branchId }
                .mapNotNull { leafCategory ->
                    val leafId = leafCategory.id ?: return@mapNotNull null
                    SelectedLeafCategory(
                        id = leafId,
                        name = leafCategory.name
                    )
                }

            ParticipantSelectedCategory(
                id = branchId,
                name = branchCategory.name,
                leafCategoryList = leafCategoriesForBranch
            )
        }
    }
}

