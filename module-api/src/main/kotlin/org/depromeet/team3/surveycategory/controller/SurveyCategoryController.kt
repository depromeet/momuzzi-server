package org.depromeet.team3.survey_category.controller

import jakarta.validation.Valid
import org.depromeet.team3.common.response.DpmApiResponse
import org.depromeet.team3.survey_category.dto.request.CreateSurveyCategoryRequest
import org.depromeet.team3.survey_category.dto.response.SurveyCategoryResponse
import org.depromeet.team3.survey_category.application.CreateSurveyCategoryService
import org.depromeet.team3.survey_category.application.GetSurveyCategoryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/survey-categories")
class SurveyCategoryController(
    private val createSurveyCategoryService: CreateSurveyCategoryService,
    private val getSurveyCategoryService: GetSurveyCategoryService
) {

    @GetMapping
    fun getSurveyCategoryList(): DpmApiResponse<SurveyCategoryResponse> {
        val response = getSurveyCategoryService()

        return DpmApiResponse.ok(response)
    }

    @PostMapping
    fun create(
        @RequestBody @Valid request: CreateSurveyCategoryRequest
    ) : DpmApiResponse<Unit> {
        createSurveyCategoryService(request)

        return DpmApiResponse.ok()
    }
}