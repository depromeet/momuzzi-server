package org.depromeet.team3.meetingattendee

import jakarta.persistence.*
import org.depromeet.team3.auth.UserEntity
import org.depromeet.team3.common.BaseTimeEntity
import org.depromeet.team3.meeting.MeetingEntity

@Entity
@Table(name = "tb_meeting_attendees")
class MeetingAttendeeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: MeetingEntity,

    @Column(name = "attendee_nickname", nullable = false)
    val attendeeNickname: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity
) : BaseTimeEntity()