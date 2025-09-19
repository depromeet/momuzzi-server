package org.depromeet.team3.survey.application

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.common.enums.SurveyCategoryType
import org.depromeet.team3.meetingattendee.MeetingAttendeeJpaRepository
import org.depromeet.team3.meeting.MeetingJpaRepository
import org.depromeet.team3.survey.Survey
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.survey.dto.request.SurveyCreateRequest
import org.depromeet.team3.survey.exception.SurveyException
import org.depromeet.team3.survey.util.SurveyTestDataFactory
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("[SURVEY] 설문 생성 서비스 테스트")
class CreateSurveyServiceTest {

    @Mock
    private lateinit var surveyRepository: SurveyRepository

    @Mock
    private lateinit var surveyResultRepository: SurveyResultRepository

    @Mock
    private lateinit var surveyCategoryRepository: SurveyCategoryRepository

    @Mock
    private lateinit var meetingJpaRepository: MeetingJpaRepository

    @Mock
    private lateinit var meetingAttendeeJpaRepository: MeetingAttendeeJpaRepository

    private lateinit var createSurveyService: CreateSurveyService

    @BeforeEach
    fun setUp() {
        createSurveyService = CreateSurveyService(
            surveyRepository,
            surveyResultRepository,
            surveyCategoryRepository,
            meetingJpaRepository,
            meetingAttendeeJpaRepository
        )
    }

    @Test
    @DisplayName("설문을 성공적으로 생성한다")
    fun `설문을 성공적으로 생성한다`() {
        // given
        val meetingId = 1L
        val userId = 1L
        val participantId = 1L
        
        val testScenario = SurveyTestDataFactory.createSurveyTestScenario(
            meetingId = meetingId,
            participantId = participantId
        )

        // Mock 설정
        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)
        whenever(meetingAttendeeJpaRepository.existsByMeetingIdAndUserId(meetingId, participantId)).thenReturn(true)
        whenever(surveyRepository.existsByMeetingIdAndParticipantId(meetingId, participantId)).thenReturn(false)
        whenever(surveyRepository.save(any())).thenReturn(testScenario.survey)
        whenever(surveyCategoryRepository.findByNameAndType("한식", SurveyCategoryType.CUISINE))
            .thenReturn(testScenario.cuisineCategory)
        whenever(surveyCategoryRepository.findByNameAndType("일식", SurveyCategoryType.CUISINE))
            .thenReturn(testScenario.japaneseCuisineCategory)
        whenever(surveyCategoryRepository.findByNameAndType("글루텐", SurveyCategoryType.AVOID_INGREDIENT))
            .thenReturn(testScenario.ingredientCategory)
        whenever(surveyCategoryRepository.findByNameAndType("내장", SurveyCategoryType.AVOID_MENU))
            .thenReturn(null)
        whenever(surveyResultRepository.saveAll(any())).thenReturn(emptyList())

        // when
        val result = createSurveyService.invoke(meetingId, userId, testScenario.request)

        // then
        assert(result.message == "설문 제출이 완료되었습니다")
        verify(surveyRepository).save(any())
        verify(surveyResultRepository).saveAll(any())
    }

    @Test
    @DisplayName("존재하지 않는 모임에 설문 생성 시 예외가 발생한다")
    fun `존재하지 않는 모임에 설문 생성 시 예외가 발생한다`() {
        // given
        val meetingId = 999L
        val userId = 1L
        val request = SurveyTestDataFactory.createMinimalSurveyCreateRequest()

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(false)

        // when & then
        val exception = assertThrows<SurveyException> {
            createSurveyService.invoke(meetingId, userId, request)
        }

        assert(exception.errorCode == ErrorCode.MEETING_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 참가자가 설문 생성 시 예외가 발생한다")
    fun `존재하지 않는 참가자가 설문 생성 시 예외가 발생한다`() {
        // given
        val meetingId = 1L
        val userId = 999L
        val participantId = 999L
        val request = SurveyTestDataFactory.createMinimalSurveyCreateRequest(participantId = participantId)

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)
        whenever(meetingAttendeeJpaRepository.existsByMeetingIdAndUserId(meetingId, participantId)).thenReturn(false)

        // when & then
        val exception = assertThrows<SurveyException> {
            createSurveyService.invoke(meetingId, userId, request)
        }

        assert(exception.errorCode == ErrorCode.PARTICIPANT_NOT_FOUND)
    }

    @Test
    @DisplayName("중복 설문 제출 시 예외가 발생한다")
    fun `중복 설문 제출 시 예외가 발생한다`() {
        // given
        val meetingId = 1L
        val userId = 1L
        val participantId = 1L
        val request = SurveyTestDataFactory.createMinimalSurveyCreateRequest(participantId = participantId)

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)
        whenever(meetingAttendeeJpaRepository.existsByMeetingIdAndUserId(meetingId, participantId)).thenReturn(true)
        whenever(surveyRepository.existsByMeetingIdAndParticipantId(meetingId, participantId)).thenReturn(true)

        // when & then
        val exception = assertThrows<SurveyException> {
            createSurveyService.invoke(meetingId, userId, request)
        }

        assert(exception.errorCode == ErrorCode.SURVEY_ALREADY_SUBMITTED)
    }

    @Test
    @DisplayName("다른 사용자의 설문을 제출하려고 할 때 예외가 발생한다")
    fun `다른 사용자의 설문을 제출하려고 할 때 예외가 발생한다`() {
        // given
        val meetingId = 1L
        val userId = 1L
        val participantId = 999L // 다른 사용자 ID
        val request = SurveyTestDataFactory.createSurveyCreateRequest(
            participantId = participantId,
            nickname = "다른사용자"
        )

        whenever(meetingJpaRepository.existsById(meetingId)).thenReturn(true)

        // when & then
        val exception = assertThrows<SurveyException> {
            createSurveyService.invoke(meetingId, userId, request)
        }

        assert(exception.errorCode == ErrorCode.PARTICIPANT_NOT_FOUND)
    }
}
