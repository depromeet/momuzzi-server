package org.depromeet.team3.restaurant

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity
import org.depromeet.team3.menu.MenuEntity

@Entity
@Table(name = "tb_restaurants")
class RestaurantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val category: String,
    
    @Column(nullable = false)
    val rating: Double,
    
    @Column(name = "review_count", nullable = false)
    val reviewCount: Int,
    
    @Column(nullable = false)
    val address: String,
    
    @Column(name = "closest_station", nullable = false)
    val closestStation: String,
    
    @Column(name = "working_hours", nullable = false)
    val workingHours: String,
    
    @Column(name = "phone_no", nullable = false)
    val phoneNo: String,
    
    @Column(columnDefinition = "TEXT")
    val descriptions: String,
    
    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false,
    
    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    val menus: MutableList<MenuEntity> = mutableListOf()
) : BaseTimeEntity()