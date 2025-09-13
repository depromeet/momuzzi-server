package org.depromeet.team3.meeting

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity
import org.depromeet.team3.station.StationEntity
import org.depromeet.team3.user.UserEntity
import java.time.LocalDateTime

@Entity
@Table(name = "tb_meetings")
class MeetingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "attendee_count", nullable = false)
    val attendeeCount: Int,
    
    @Column(name = "is_closed", nullable = false)
    val isClosed: Boolean = false,
    
    @Column(name = "end_at")
    val endAt: LocalDateTime? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    val hostUser: UserEntity,
    
    @OneToOne(mappedBy = "meeting", fetch = FetchType.LAZY)
    val station: StationEntity,
    
    @OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
    val attendees: MutableList<MeetingAttendeeEntity> = mutableListOf()
) : BaseTimeEntity()
