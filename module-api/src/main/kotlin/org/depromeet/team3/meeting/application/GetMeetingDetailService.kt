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

        // MeetingInfoResponse 생성
        val meetingInfo = MeetingInfoResponse(
            id = meeting.id!!,
            title = meeting.name,
            hostUserId = meeting.hostUserId,
            totalParticipantCnt = meeting.attendeeCount,
            isClosed = meeting.isClosed,
            stationName = stationName,
            endAt = meeting.endAt!!,
            createdAt = meeting.createdAt!!,
            updatedAt = meeting.updatedAt
        )

        // 참가자 목록 조회
        val attendees = meetingAttendeeRepository.findByMeetingId(meetingId)
        
        // 설문이 있는 참가자만 participantList에 포함
        val participantList = attendees
            .mapNotNull { attendee ->
                // 해당 참가자의 설문 조회
                val survey = surveyRepository.findByMeetingIdAndParticipantId(meetingId, attendee.userId)
                
                // 설문이 없는 경우 null 반환하여 제외
                survey ?: return@mapNotNull null
                
                // 설문이 있는 경우 선택한 카테고리 목록 생성
                val selectedCategoryList = buildParticipantSelectedCategories(survey.id!!)

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
        val categoryIds = surveyResults.map { it.surveyCategoryId }
        val categories = categoryIds.mapNotNull { surveyCategoryRepository.findById(it) }

        // BRANCH 카테고리와 LEAF 카테고리 분리
        val branchCategories = categories.filter { it.level == SurveyCategoryLevel.BRANCH }
        val leafCategories = categories.filter { it.level == SurveyCategoryLevel.LEAF }

        // BRANCH 카테고리별로 해당하는 LEAF 카테고리들을 그룹화
        return branchCategories.map { branchCategory ->
            val leafCategoriesForBranch = leafCategories
                .filter { it.parentId == branchCategory.id }
                .map { leafCategory ->
                    SelectedLeafCategory(
                        id = leafCategory.id!!,
                        name = leafCategory.name
                    )
                }

            ParticipantSelectedCategory(
                id = branchCategory.id!!,
                name = branchCategory.name,
                leafCategoryList = leafCategoriesForBranch
            )
        }
    }
}

