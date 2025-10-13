package org.depromeet.team3.placelike

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity
import org.depromeet.team3.meetingplace.MeetingPlaceEntity

@Entity
@Table(
    name = "tb_place_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_place_like",
            columnNames = ["meeting_place_id", "user_id"]
        )
    ]
)
class PlaceLikeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_place_id", nullable = false)
    val meetingPlace: MeetingPlaceEntity,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseTimeEntity()