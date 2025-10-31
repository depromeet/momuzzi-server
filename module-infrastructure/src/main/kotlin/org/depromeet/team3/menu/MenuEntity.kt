package org.depromeet.team3.menu

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity
import org.depromeet.team3.place.PlaceEntity

@Entity
@Table(name = "tb_menus")
class MenuEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val category: String,
    
    @Column(nullable = false)
    val price: Int,
    
    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val place: PlaceEntity
) : BaseTimeEntity()
