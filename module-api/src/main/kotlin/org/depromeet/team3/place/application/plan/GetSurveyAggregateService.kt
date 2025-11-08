package org.depromeet.team3.place.application.plan

import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meeting.MeetingQuery
import org.depromeet.team3.place.application.model.PlaceSurveySummary
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.station.StationRepository
import org.depromeet.team3.survey.SurveyRepository
import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import org.depromeet.team3.surveycategory.SurveyCategoryRepository
import org.depromeet.team3.surveyresult.SurveyResultRepository
import org.springframework.stereotype.Service

/**
 * 모임 → 역 → 설문 → 카테고리 순으로 존재 여부 확인
 * 설문 응답 결과를 branch / leaf 단위로 그룹화 및 득표수 집계
 */
@Service
class GetSurveyAggregateService(
    private val meetingQuery: MeetingQuery,
    private val surveyRepository: SurveyRepository,
    private val surveyResultRepository: SurveyResultRepository,
    private val surveyCategoryRepository: SurveyCategoryRepository,
    private val stationRepository: StationRepository
) {

    fun load(meetingId: Long): PlaceSurveySummary {
        val meeting = meetingQuery.findById(meetingId)
            ?: throw PlaceSearchException(ErrorCode.MEETING_NOT_FOUND, mapOf("meetingId" to meetingId))

        val station = stationRepository.findById(meeting.stationId)
            ?: throw PlaceSearchException(ErrorCode.STATION_NOT_FOUND, mapOf("stationId" to meeting.stationId))

        val stationCoordinates = meetingQuery.getStationCoordinates(meetingId)

        val surveys = surveyRepository.findByMeetingId(meetingId)
        if (surveys.isEmpty()) {
            throw PlaceSearchException(ErrorCode.SURVEY_NOT_FOUND, mapOf("meetingId" to meetingId))
        }

        val surveyIds = surveys.mapNotNull { it.id }
        val surveyResults = surveyResultRepository.findBySurveyIdIn(surveyIds)
        if (surveyResults.isEmpty()) {
            throw PlaceSearchException(ErrorCode.SURVEY_RESULT_NOT_FOUND, mapOf("meetingId" to meetingId))
        }

        val categoryIds = surveyResults.map { it.surveyCategoryId }.distinct()
        if (categoryIds.isEmpty()) {
            throw PlaceSearchException(ErrorCode.SURVEY_CATEGORY_NOT_FOUND, mapOf("meetingId" to meetingId))
        }

        val categories = surveyCategoryRepository.findAllById(categoryIds).toList()
        if (categories.isEmpty()) {
            throw PlaceSearchException(ErrorCode.SURVEY_CATEGORY_NOT_FOUND, mapOf("meetingId" to meetingId))
        }

        val branchCategories = mutableMapOf<Long, SurveyCategory>()
        val leafCategories = mutableMapOf<Long, SurveyCategory>()
        categories.forEach { category ->
            val id = category.id ?: return@forEach
            when (category.level) {
                SurveyCategoryLevel.BRANCH -> branchCategories[id] = category
                SurveyCategoryLevel.LEAF -> if (category.parentId != null) {
                    leafCategories[id] = category
                }
            }
        }

        val resultsBySurvey = surveyResults.groupBy { it.surveyId }
        val totalRespondents = resultsBySurvey.size
        if (totalRespondents == 0) {
            throw PlaceSearchException(ErrorCode.SURVEY_RESULT_NOT_FOUND, mapOf("meetingId" to meetingId))
        }

        val branchVotes = mutableMapOf<Long, Int>()
        val leafVotes = mutableMapOf<Long, Int>()

        resultsBySurvey.values.forEach { answers ->
            val uniqueCategoryIds = answers.map { it.surveyCategoryId }.toSet()
            val branchSet = mutableSetOf<Long>()
            val leafSet = mutableSetOf<Long>()

            uniqueCategoryIds.forEach { categoryId ->
                when {
                    branchCategories.containsKey(categoryId) -> branchSet.add(categoryId)
                    leafCategories.containsKey(categoryId) -> leafSet.add(categoryId)
                }
            }

            branchSet.forEach { branchId ->
                branchVotes[branchId] = (branchVotes[branchId] ?: 0) + 1
            }
            leafSet.forEach { leafId ->
                leafVotes[leafId] = (leafVotes[leafId] ?: 0) + 1
            }
        }

        return PlaceSurveySummary(
            stationName = station.name,
            stationCoordinates = stationCoordinates,
            totalRespondents = totalRespondents,
            leafVotes = leafVotes,
            branchVotes = branchVotes,
            leafCategories = leafCategories,
            branchCategories = branchCategories
        )
    }
}