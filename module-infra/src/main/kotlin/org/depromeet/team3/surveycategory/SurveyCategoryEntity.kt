package org.depromeet.team3.survey_category

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity

@Entity
@Table(name = "tb_survey_category_master")
class SurveyCategoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: SurveyCategoryEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: SurveyCategoryType,

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    val level: SurveyCategoryLevel,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "`order`", nullable = false) // order는 예약어이므로 백틱 처리
    val order: Int,

    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false
) : BaseTimeEntity()
