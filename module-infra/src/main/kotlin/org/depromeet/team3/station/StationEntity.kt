package org.depromeet.team3.station

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity

@Entity
@Table(name = "tb_stations")
class StationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(name = "loc_x", nullable = false)
    val locX: Double,
    
    @Column(name = "loc_y", nullable = false)
    val locY: Double,
    
    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false,
) : BaseTimeEntity()