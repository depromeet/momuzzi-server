package org.depromeet.team3.user

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity

@Entity
@Table(name = "tb_users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    @Column(name = "social_id", nullable = false, unique = true)
    var socialId: String = "",
    
    @Column(nullable = false, unique = true)
    var email: String = "",
    
    @Column(nullable = false)
    var nickname: String = "",
    
    @Column(name = "profile_image")
    var profileImage: String? = null,
    
    @Column(name = "refresh_token")
    var refreshToken: String? = null,
) : BaseTimeEntity()
