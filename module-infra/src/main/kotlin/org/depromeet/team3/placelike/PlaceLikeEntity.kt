package org.depromeet.team3.placelike

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity

@Entity
@Table(
    name = "tb_place_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_place_like",
            columnNames = ["meeting_place_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_meeting_place_id", columnList = "meeting_place_id"),
        Index(name = "idx_user_id", columnList = "user_id")
    ]
)
class PlaceLikeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "meeting_place_id", nullable = false)
    val meetingPlaceId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseTimeEntity()
