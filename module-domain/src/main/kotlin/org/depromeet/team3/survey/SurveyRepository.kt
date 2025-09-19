package org.depromeet.team3.survey

interface SurveyRepository {
    fun save(survey: Survey): Survey
    fun findByMeetingIdAndParticipantId(meetingId: Long, participantId: Long): Survey?
    fun findByMeetingId(meetingId: Long): List<Survey>
    fun existsByMeetingIdAndParticipantId(meetingId: Long, participantId: Long): Boolean
}
