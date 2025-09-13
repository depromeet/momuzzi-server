package org.depromeet.team3.meeting

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity
import java.time.LocalDateTime

@Entity
@Table(name = "tb_meetings")
class MeetingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "host_user_id", nullable = false)
    val hostUserId: Long,
    
    @Column(name = "attendee_count", nullable = false)
    val attendeeCount: Int,
    
    @Column(name = "is_closed", nullable = false)
    val isClosed: Boolean = false,
    
    @Column(name = "end_at")
    val endAt: LocalDateTime? = null,
) : BaseTimeEntity()
