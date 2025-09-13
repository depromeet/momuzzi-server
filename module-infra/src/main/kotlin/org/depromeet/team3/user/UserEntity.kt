package org.depromeet.team3.user

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity

@Entity
@Table(name = "tb_users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "kakao_id", nullable = false, unique = true)
    val kakaoId: String,
    
    @Column(nullable = false)
    val email: String,
    
    @Column(nullable = false)
    val nickname: String,
) : BaseTimeEntity()
