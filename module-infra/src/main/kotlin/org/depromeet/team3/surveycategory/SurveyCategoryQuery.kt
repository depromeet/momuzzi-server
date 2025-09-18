package org.depromeet.team3.survey_category

import org.depromeet.team3.mapper.SurveyCategoryMapper
import org.springframework.stereotype.Repository

@Repository
class SurveyCategoryQuery (
    private val surveyCategoryMapper: SurveyCategoryMapper,
    private val surveyCategoryJpaRepository: SurveyCategoryJpaRepository
) : SurveyCategoryRepository {

    override fun save(surveyCategory: SurveyCategory): SurveyCategory {
        val entity = surveyCategoryMapper.toEntity(surveyCategory)
        return surveyCategoryMapper.toDomain(surveyCategoryJpaRepository.save(entity))
    }

    override fun findActive(): List<SurveyCategory> {
        return surveyCategoryJpaRepository.findByIsDeletedFalse()
            .map { surveyCategoryMapper.toDomain(it) }
    }
}