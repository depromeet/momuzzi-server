package org.depromeet.team3.survey

import org.depromeet.team3.mapper.SurveyMapper
import org.springframework.stereotype.Repository

@Repository
class SurveyQuery(
    private val surveyMapper: SurveyMapper,
    private val surveyJpaRepository: SurveyJpaRepository
) : SurveyRepository {
    
    override fun save(survey: Survey): Survey {
        val entity = surveyMapper.toEntity(survey)
        return surveyMapper.toDomain(surveyJpaRepository.save(entity))
    }
    
    override fun findByMeetingIdAndParticipantId(meetingId: Long, participantId: Long): Survey? {
        return surveyJpaRepository.findByMeetingIdAndParticipantId(meetingId, participantId)
            ?.let { surveyMapper.toDomain(it) }
    }
    
    override fun findByMeetingId(meetingId: Long): List<Survey> {
        return surveyJpaRepository.findByMeetingId(meetingId)
            .map { surveyMapper.toDomain(it) }
    }
    
    override fun existsByMeetingIdAndParticipantId(meetingId: Long, participantId: Long): Boolean {
        return surveyJpaRepository.existsByMeetingIdAndParticipantId(meetingId, participantId)
    }
}
