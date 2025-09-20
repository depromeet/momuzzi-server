package org.depromeet.team3.survey.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.enums.SurveyCategoryType
import org.depromeet.team3.meetingattendee.MeetingAttendee
import org.depromeet.team3.meetingattendee.MeetingAttendeeRepository
import org.depromeet.team3.meetingattendee.util.MeetingAttendeeTestDataFactory
import org.depromeet.team3.meeting.MeetingEntity
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.meeting.util.MeetingTestDataFactory
import org.depromeet.team3.station.StationEntity
import org.depromeet.team3.station.util.StationTestDataFactory
import org.depromeet.team3.user.UserEntity
import org.depromeet.team3.user.util.UserTestDataFactory
import org.depromeet.team3.survey.Survey
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.survey.exception.SurveyException
import org.depromeet.team3.survey.util.SurveyTestDataFactory
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveyresult.SurveyResult
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

@ExtendWith(MockitoExtension::class)
@DisplayName("[SURVEY] 설문 목록 조회 서비스 테스트")
class GetSurveyListServiceTest {

    @Mock
    private lateinit var surveyRepository: SurveyRepository

    @Mock
    private lateinit var surveyResultRepository: SurveyResultRepository

    @Mock
    private lateinit var surveyCategoryRepository: SurveyCategoryRepository

    @Mock
    private lateinit var meetingJpaRepository: MeetingJpaRepository

    @Mock
    private lateinit var meetingAttendeeRepository: MeetingAttendeeRepository

    private lateinit var getSurveyListService: GetSurveyListService

    @BeforeEach
    fun setUp() {
        getSurveyListService = GetSurveyListService(
            surveyRepository,
            surveyResultRepository,
            surveyCategoryRepository,
            meetingJpaRepository,
            meetingAttendeeRepository
        )
    }

    @Test
    @DisplayName("설문 목록을 성공적으로 조회한다")
    fun `설문 목록을 성공적으로 조회한다`() {
        // given
        val meetingId = 1L
        val userId = 1L
        val participantId1 = 1L
        val participantId2 = 2L

        val meetingEntity = MeetingTestDataFactory.createMeetingEntity(
            id = meetingId,
            hostUser = UserTestDataFactory.createUserEntity(nickname = "테스트호스트"),
            station = StationTestDataFactory.createStationEntity(name = "테스트역")
        )

        val attendee = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            meetingId = meetingId,
            userId = userId,
            attendeeNickname = "조회자"
        )

        val survey1 = SurveyTestDataFactory.createSurvey(
            id = 1L,
            meetingId = meetingId,
            participantId = participantId1
        )

        val survey2 = SurveyTestDataFactory.createSurvey(
            id = 2L,
            meetingId = meetingId,
            participantId = participantId2
        )

        val surveyResults1 = SurveyResultTestDataFactory.createSurveyResultList(
            surveyId = 1L,
            categoryIds = listOf(1L, 2L)
        )

        val surveyResults2 = SurveyResultTestDataFactory.createSurveyResultList(
            surveyId = 2L,
            categoryIds = listOf(3L)
        )

        val cuisineCategory = SurveyTestDataFactory.createSurveyCategory(
            id = 1L,
            type = SurveyCategoryType.CUISINE,
            name = "한식"
        )
        val ingredientCategory = SurveyTestDataFactory.createSurveyCategory(
            id = 2L,
            type = SurveyCategoryType.AVOID_INGREDIENT,
            name = "글루텐"
        )
        val menuCategory = SurveyTestDataFactory.createSurveyCategory(
            id = 3L,
            type = SurveyCategoryType.AVOID_MENU,
            name = "내장"
        )

        val participant1 = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 1L,
            meetingId = meetingId,
            userId = participantId1,
            attendeeNickname = "참가자1"
        )

        val participant2 = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 2L,
            meetingId = meetingId,
            userId = participantId2,
            attendeeNickname = "참가자2"
        )

        // Mock 설정
        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)
        whenever(meetingAttendeeRepository.existsByMeetingIdAndUserId(meetingId, userId)).thenReturn(true)
        whenever(surveyRepository.findByMeetingId(meetingId)).thenReturn(listOf(survey1, survey2))
        whenever(surveyResultRepository.findBySurveyId(1L)).thenReturn(surveyResults1)
        whenever(surveyResultRepository.findBySurveyId(2L)).thenReturn(surveyResults2)
        whenever(surveyCategoryRepository.findById(1L)).thenReturn(cuisineCategory)
        whenever(surveyCategoryRepository.findById(2L)).thenReturn(ingredientCategory)
        whenever(surveyCategoryRepository.findById(3L)).thenReturn(menuCategory)
        whenever(meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, participantId1)).thenReturn(participant1)
        whenever(meetingAttendeeRepository.findByMeetingIdAndUserId(meetingId, participantId2)).thenReturn(participant2)

        // when
        val result = getSurveyListService.invoke(meetingId, userId)

        // then
        assert(result.surveys.size == 2)
        
        val surveyItem1 = result.surveys.find { it.participantId == participantId1 }
        assert(surveyItem1 != null)
        assert(surveyItem1!!.nickname == "참가자1")
        assert(surveyItem1.preferredCuisineList.contains("한식"))
        assert(surveyItem1.avoidIngredientList.contains("글루텐"))
        assert(surveyItem1.avoidMenuList.isEmpty())

        val surveyItem2 = result.surveys.find { it.participantId == participantId2 }
        assert(surveyItem2 != null)
        assert(surveyItem2!!.nickname == "참가자2")
        assert(surveyItem2.preferredCuisineList.isEmpty())
        assert(surveyItem2.avoidIngredientList.isEmpty())
        assert(surveyItem2.avoidMenuList.contains("내장"))
    }

    @Test
    @DisplayName("존재하지 않는 모임 조회 시 예외가 발생한다")
    fun `존재하지 않는 모임 조회 시 예외가 발생한다`() {
        // given
        val meetingId = 999L
        val userId = 1L

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(false)

        // when & then
        val exception = assertThrows<SurveyException> {
            getSurveyListService.invoke(meetingId, userId)
        }

        assert(exception.errorCode == ErrorCode.MEETING_NOT_FOUND)
    }

    @Test
    @DisplayName("모임 참가자가 아닌 사용자가 조회 시 예외가 발생한다")
    fun `모임 참가자가 아닌 사용자가 조회 시 예외가 발생한다`() {
        // given
        val meetingId = 1L
        val userId = 999L

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)
        whenever(meetingAttendeeRepository.existsByMeetingIdAndUserId(meetingId, userId)).thenReturn(false)

        // when & then
        val exception = assertThrows<SurveyException> {
            getSurveyListService.invoke(meetingId, userId)
        }

        assert(exception.errorCode == ErrorCode.PARTICIPANT_NOT_FOUND)
    }

    @Test
    @DisplayName("설문이 없는 모임 조회 시 빈 목록을 반환한다")
    fun `설문이 없는 모임 조회 시 빈 목록을 반환한다`() {
        // given
        val meetingId = 1L
        val userId = 1L

        val attendee = MeetingAttendeeTestDataFactory.createMeetingAttendee(
            id = 1L,
            meetingId = meetingId,
            userId = userId,
            attendeeNickname = "조회자"
        )

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)
        whenever(meetingAttendeeRepository.existsByMeetingIdAndUserId(meetingId, userId)).thenReturn(true)
        whenever(surveyRepository.findByMeetingId(meetingId)).thenReturn(emptyList())

        // when
        val result = getSurveyListService.invoke(meetingId, userId)

        // then
        assert(result.surveys.isEmpty())
    }
}
