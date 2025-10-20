package org.depromeet.team3.survey.dto.response

data class SurveyCreateResponse(
    val message: String = "설문 제출이 완료되었습니다"
)

data class SurveyItemResponse(
    val participantId: Long,
    val nickname: String,
    val selectedCategoryList: List<Long>
)

data class SurveyListResponse(
    val surveys: List<SurveyItemResponse>
)
