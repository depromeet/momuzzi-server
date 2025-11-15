package org.depromeet.team3.meetingplacesearch

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity
import java.time.LocalDateTime

/**
 * 모임별 장소 검색 결과 저장
 * 
 * - 검색 결과 전체를 JSON으로 저장
 * - expiresAt (meeting.endAt + 6시간) 이후 배치로 삭제
 */
@Entity
@Table(
    name = "tb_meeting_place_searches",
    indexes = [
        Index(name = "idx_meeting_id", columnList = "meeting_id", unique = true),
        Index(name = "idx_expires_at", columnList = "expires_at")
    ]
)
class MeetingPlaceSearchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "meeting_id", nullable = false, unique = true)
    val meetingId: Long,

    @Column(name = "search_result_json", columnDefinition = "TEXT", nullable = false)
    val searchResultJson: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime
) : BaseTimeEntity()
