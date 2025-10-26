package org.depromeet.team3.meeting.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.dto.response.*
import org.depromeet.team3.meeting.exception.MeetingException
import org.depromeet.team3.meeting.util.MeetingTestDataFactory
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.MuzziColor
import org.depromeet.team3.meetingattendee.util.MeetingAttendeeTestDataFactory
import org.depromeet.team3.station.StationRepository
import org.depromeet.team3.station.util.StationTestDataFactory
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.survey.util.SurveyTestDataFactory
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.util.SurveyCategoryTestDataFactory
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.depromeet.team3.surveyresult.util.SurveyResultTestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("[MEETING] 모임 상세 조회 서비스 테스트")
class GetMeetingDetailServiceTest {

    @Mock
    private lateinit var meetingRepository: MeetingRepository

    @Mock
    private lateinit var stationRepository: StationRepository

    @Mock
    private lateinit var meetingAttendeeRepository: MeetingAttendeeRepository

    @Mock
    private lateinit var surveyRepository: SurveyRepository

    @Mock
    private lateinit var surveyResultRepository: SurveyResultRepository

    @Mock
    private lateinit var surveyCategoryRepository: SurveyCategoryRepository

    private lateinit var getMeetingDetailService: GetMeetingDetailService

    @BeforeEach
    fun setUp() {
        getMeetingDetailService = GetMeetingDetailService(
            meetingRepository,
            stationRepository,
            meetingAttendeeRepository,
            surveyRepository,
            surveyResultRepository,
            surveyCategoryRepository
        )
    }

    @Test
    @DisplayName("모임 상세 정보를 성공적으로 조회한다")
    fun `모임 상세 정보를 성공적으로 조회한다`() {
        // given
        val meetingId = 1L
        val userId = 234L
        val meeting = MeetingTestDataFactory.createMeeting(
            id = meetingId,
            name = "점심 메뉴 정하기",
            hostUserId = 123L,
            attendeeCount = 8,
            isClosed = false,
            stationId = 1L,
            endAt = LocalDateTime.now().plusHours(2)
        )

        val station = StationTestDataFactory.createStation(
            id = 1L,
            name = "강남"
        )

        val attendee1 = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 1L,
            meetingId = meetingId,
            userId = 456L,
            attendeeNickname = "아따맘마",
            muzziColor = MuzziColor.CHOCOLATE
        )

        val attendee2 = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 2L,
            meetingId = meetingId,
            userId = 789L,
            attendeeNickname = "아따맘마마마",
            muzziColor = MuzziColor.ORANGE
        )

        val survey1 = SurveyTestDataFactory.createSurvey(
            id = 1L,
            meetingId = meetingId,
            participantId = 456L
        )

        val survey2 = SurveyTestDataFactory.createSurvey(
            id = 2L,
            meetingId = meetingId,
            participantId = 789L
        )

        // Mock 설정
        whenever(meetingRepository.findById(meetingId)).thenReturn(meeting)
        whenever(stationRepository.findById(1L)).thenReturn(station)
        whenever(meetingAttendeeRepository.findByMeetingId(meetingId)).thenReturn(listOf(attendee1, attendee2))
        whenever(surveyRepository.findByMeetingIdAndParticipantId(meetingId, 456L)).thenReturn(survey1)
        whenever(surveyRepository.findByMeetingIdAndParticipantId(meetingId, 789L)).thenReturn(survey2)
        whenever(surveyResultRepository.findBySurveyId(1L)).thenReturn(emptyList())
        whenever(surveyResultRepository.findBySurveyId(2L)).thenReturn(emptyList())

        // when
        val result = getMeetingDetailService.invoke(meetingId, userId)

        // then
        assert(result.currentUserId == userId)
        assert(result.meetingInfo.title == "점심 메뉴 정하기")
        assert(result.meetingInfo.hostUserId == 123L)
        assert(result.meetingInfo.totalParticipantCnt == 8)
        assert(result.meetingInfo.stationName == "강남")
        assert(result.participantList.size == 2)
        
        val participant1 = result.participantList.find { it.userId == 456L }
        assert(participant1 != null)
        assert(participant1!!.nickname == "아따맘마")
        assert(participant1.profileColor == "chocolate")
    }

    @Test
    @DisplayName("참가자의 설문 정보를 포함하여 조회한다")
    fun `참가자의 설문 정보를 포함하여 조회한다`() {
        // given
        val meetingId = 1L
        val userId = 234L
        val meeting = MeetingTestDataFactory.createMeeting(
            id = meetingId,
            name = "저녁 모임",
            hostUserId = 123L,
            attendeeCount = 2,
            isClosed = false,
            stationId = 1L,
            endAt = LocalDateTime.now().plusHours(2)
        )

        val station = StationTestDataFactory.createStation(
            id = 1L,
            name = "강남"
        )

        val attendee = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 1L,
            meetingId = meetingId,
            userId = 456L,
            attendeeNickname = "참가자1",
            muzziColor = MuzziColor.DEFAULT
        )

        val survey = SurveyTestDataFactory.createSurvey(
            id = 1L,
            meetingId = meetingId,
            participantId = 456L
        )

        // BRANCH 카테고리 (한식)
        val branchCategory = SurveyCategoryTestDataFactory.createBranchCategory(
            id = 1L,
            name = "한식"
        )

        // LEAF 카테고리들
        val leafCategory1 = SurveyCategoryTestDataFactory.createLeafCategory(
            id = 8L,
            parentId = 1L,
            name = "밥류"
        )

        val leafCategory2 = SurveyCategoryTestDataFactory.createLeafCategory(
            id = 9L,
            parentId = 1L,
            name = "구이·조림류"
        )

        val surveyResults = SurveyResultTestDataFactory.createSurveyResultList(
            surveyId = 1L,
            categoryIds = listOf(1L, 8L, 9L)
        )

        // Mock 설정
        whenever(meetingRepository.findById(meetingId)).thenReturn(meeting)
        whenever(stationRepository.findById(1L)).thenReturn(station)
        whenever(meetingAttendeeRepository.findByMeetingId(meetingId)).thenReturn(listOf(attendee))
        whenever(surveyRepository.findByMeetingIdAndParticipantId(meetingId, 456L)).thenReturn(survey)
        whenever(surveyResultRepository.findBySurveyId(1L)).thenReturn(surveyResults)
        whenever(surveyCategoryRepository.findById(1L)).thenReturn(branchCategory)
        whenever(surveyCategoryRepository.findById(8L)).thenReturn(leafCategory1)
        whenever(surveyCategoryRepository.findById(9L)).thenReturn(leafCategory2)

        // when
        val result = getMeetingDetailService.invoke(meetingId, userId)

        // then
        assert(result.participantList.isNotEmpty())
        val participant = result.participantList[0]
        assert(participant.selectedCategories.isNotEmpty())
        
        val selectedCategory = participant.selectedCategories[0]
        assert(selectedCategory.id == 1L)
        assert(selectedCategory.name == "한식")
        assert(selectedCategory.leafCategoryList.size == 2)
        assert(selectedCategory.leafCategoryList.any { it.id == 8L && it.name == "밥류" })
        assert(selectedCategory.leafCategoryList.any { it.id == 9L && it.name == "구이·조림류" })
    }

    @Test
    @DisplayName("존재하지 않는 모임 조회 시 예외가 발생한다")
    fun `존재하지 않는 모임 조회 시 예외가 발생한다`() {
        // given
        val meetingId = 999L
        val userId = 234L

        whenever(meetingRepository.findById(meetingId)).thenReturn(null)

        // when & then
        val exception = assertThrows<MeetingException> {
            getMeetingDetailService.invoke(meetingId, userId)
        }

        assert(exception.errorCode == ErrorCode.MEETING_NOT_FOUND)
    }

    @Test
    @DisplayName("설문이 없는 참가자는 participantList에 포함되지 않는다")
    fun `설문이 없는 참가자는 participantList에 포함되지 않는다`() {
        // given
        val meetingId = 1L
        val userId = 234L
        val meeting = MeetingTestDataFactory.createMeeting(
            id = meetingId,
            name = "테스트 모임",
            hostUserId = 123L,
            attendeeCount = 2,
            isClosed = false,
            stationId = 1L,
            endAt = LocalDateTime.now().plusHours(2)
        )

        val station = StationTestDataFactory.createStation(
            id = 1L,
            name = "강남"
        )

        val attendee1 = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 1L,
            meetingId = meetingId,
            userId = 456L,
            attendeeNickname = "설문한 참가자",
            muzziColor = MuzziColor.DEFAULT
        )

        val attendee2 = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 2L,
            meetingId = meetingId,
            userId = 789L,
            attendeeNickname = "설문 안 한 참가자",
            muzziColor = MuzziColor.ORANGE
        )

        val survey1 = SurveyTestDataFactory.createSurvey(
            id = 1L,
            meetingId = meetingId,
            participantId = 456L
        )

        // Mock 설정
        whenever(meetingRepository.findById(meetingId)).thenReturn(meeting)
        whenever(stationRepository.findById(1L)).thenReturn(station)
        whenever(meetingAttendeeRepository.findByMeetingId(meetingId)).thenReturn(listOf(attendee1, attendee2))
        whenever(surveyRepository.findByMeetingIdAndParticipantId(meetingId, 456L)).thenReturn(survey1)
        whenever(surveyRepository.findByMeetingIdAndParticipantId(meetingId, 789L)).thenReturn(null)
        whenever(surveyResultRepository.findBySurveyId(1L)).thenReturn(emptyList())

        // when
        val result = getMeetingDetailService.invoke(meetingId, userId)

        // then
        assert(result.participantList.size == 1) // 설문이 있는 참가자만 포함
        assert(result.participantList[0].userId == 456L)
        assert(result.participantList[0].nickname == "설문한 참가자")
    }
}

