package org.depromeet.team3.place.application.plan

import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.meeting.MeetingQuery
import org.depromeet.team3.place.application.model.PlaceSurveySummary
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SelectSurveyKeywordsServiceTest {

    private lateinit var selectSurveyKeywordsService: SelectSurveyKeywordsService

    @BeforeEach
    fun setUp() {
        selectSurveyKeywordsService = SelectSurveyKeywordsService()
    }

    @Test
    fun `전원 선택 LEAF가 있으면 가중치 1_0으로 최우선 반환`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 5,
            leafVotes = mapOf(
                101L to 5,  // 전원 선택
                102L to 3   // 60%
            ),
            branchVotes = emptyMap(),
            leafCategories = mapOf(
                101L to createCategory(101L, "이탈리안", SurveyCategoryLevel.LEAF, sortOrder = 1),
                102L to createCategory(102L, "일식", SurveyCategoryLevel.LEAF, sortOrder = 2)
            ),
            branchCategories = emptyMap()
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then
        assertThat(result).isNotEmpty
        assertThat(result[0].keyword).isEqualTo("강남 이탈리안 맛집")
        assertThat(result[0].weight).isEqualTo(1.0)
    }

    @Test
    fun `60% 이상 득표 LEAF가 우선 선택됨`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 5,
            leafVotes = mapOf(
                101L to 4,  // 80%
                102L to 3,  // 60%
                103L to 2   // 40% - 제외되지만 4단계에서 추가될 수 있음
            ),
            branchVotes = emptyMap(),
            leafCategories = mapOf(
                101L to createCategory(101L, "이탈리안", SurveyCategoryLevel.LEAF),
                102L to createCategory(102L, "일식", SurveyCategoryLevel.LEAF),
                103L to createCategory(103L, "한식", SurveyCategoryLevel.LEAF)
            ),
            branchCategories = emptyMap()
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(2)
        assertThat(result.map { it.keyword }).contains("강남 이탈리안 맛집", "강남 일식 맛집")
        // 상위 2개의 가중치 확인
        val italian = result.find { it.keyword == "강남 이탈리안 맛집" }
        val japanese = result.find { it.keyword == "강남 일식 맛집" }
        assertThat(italian?.weight).isEqualTo(0.8)
        assertThat(japanese?.weight).isEqualTo(0.6)
    }

    @Test
    fun `50% 이상 득표 BRANCH가 선택됨`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 5,
            leafVotes = emptyMap(),
            branchVotes = mapOf(
                201L to 3,  // 60%
                202L to 2   // 40% - 제외
            ),
            leafCategories = emptyMap(),
            branchCategories = mapOf(
                201L to createCategory(201L, "양식", SurveyCategoryLevel.BRANCH),
                202L to createCategory(202L, "아시안", SurveyCategoryLevel.BRANCH)
            )
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then
        assertThat(result.map { it.keyword }).contains("강남 양식 맛집")
        assertThat(result.map { it.keyword }).doesNotContain("강남 아시안 맛집")
    }

    @Test
    fun `키워드가 부족하면 득표 상위 LEAF로 채움`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 10,
            leafVotes = mapOf(
                101L to 5,  // 50% - 60% 미만이라 2단계 제외
                102L to 4,  // 40%
                103L to 3   // 30%
            ),
            branchVotes = emptyMap(),
            leafCategories = mapOf(
                101L to createCategory(101L, "이탈리안", SurveyCategoryLevel.LEAF),
                102L to createCategory(102L, "일식", SurveyCategoryLevel.LEAF),
                103L to createCategory(103L, "한식", SurveyCategoryLevel.LEAF)
            ),
            branchCategories = emptyMap()
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then - 득표 순으로 채워짐
        assertThat(result).hasSizeGreaterThanOrEqualTo(3)
        assertThat(result.map { it.keyword }).contains(
            "강남 이탈리안 맛집",
            "강남 일식 맛집",
            "강남 한식 맛집"
        )
    }

    @Test
    fun `키워드가 없거나 부족하면 일반 키워드 추가`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 5,
            leafVotes = mapOf(101L to 2),  // 40% - 임계값 미달
            branchVotes = mapOf(201L to 2),  // 40% - 임계값 미달
            leafCategories = mapOf(
                101L to createCategory(101L, "이탈리안", SurveyCategoryLevel.LEAF)
            ),
            branchCategories = mapOf(
                201L to createCategory(201L, "양식", SurveyCategoryLevel.BRANCH)
            )
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then
        assertThat(result).isNotEmpty
        assertThat(result.last().keyword).isEqualTo("강남 맛집")
        assertThat(result.last().weight).isEqualTo(0.1)
    }

    @Test
    fun `응답자가 0명이면 빈 리스트 반환`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 0,
            leafVotes = emptyMap(),
            branchVotes = emptyMap(),
            leafCategories = emptyMap(),
            branchCategories = emptyMap()
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `최대 5개 키워드만 반환`() {
        // given
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 10,
            leafVotes = mapOf(
                101L to 10,
                102L to 9,
                103L to 8,
                104L to 7,
                105L to 6,
                106L to 5,
                107L to 4
            ),
            branchVotes = emptyMap(),
            leafCategories = mapOf(
                101L to createCategory(101L, "이탈리안", SurveyCategoryLevel.LEAF),
                102L to createCategory(102L, "일식", SurveyCategoryLevel.LEAF),
                103L to createCategory(103L, "한식", SurveyCategoryLevel.LEAF),
                104L to createCategory(104L, "중식", SurveyCategoryLevel.LEAF),
                105L to createCategory(105L, "분식", SurveyCategoryLevel.LEAF),
                106L to createCategory(106L, "양식", SurveyCategoryLevel.LEAF),
                107L to createCategory(107L, "카페", SurveyCategoryLevel.LEAF)
            ),
            branchCategories = emptyMap()
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then
        assertThat(result).hasSize(5)
    }

    @Test
    fun `중복 키워드는 제거됨`() {
        // given - LEAF와 BRANCH가 같은 이름인 경우
        val summary = PlaceSurveySummary(
            stationName = "강남",
            stationCoordinates = null,
            totalRespondents = 5,
            leafVotes = mapOf(101L to 4),  // 80%
            branchVotes = mapOf(201L to 3),  // 60%
            leafCategories = mapOf(
                101L to createCategory(101L, "양식", SurveyCategoryLevel.LEAF)
            ),
            branchCategories = mapOf(
                201L to createCategory(201L, "양식", SurveyCategoryLevel.BRANCH)
            )
        )

        // when
        val result = selectSurveyKeywordsService.selectKeywords(summary)

        // then - "강남 양식 맛집"은 한 번만 포함
        val yangshikCount = result.count { it.keyword == "강남 양식 맛집" }
        assertThat(yangshikCount).isEqualTo(1)
    }

    private fun createCategory(
        id: Long,
        name: String,
        level: SurveyCategoryLevel,
        parentId: Long? = null,
        sortOrder: Int = 0
    ): SurveyCategory {
        return SurveyCategory(
            id = id,
            parentId = parentId,
            level = level,
            name = name,
            sortOrder = sortOrder,
            isDeleted = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}