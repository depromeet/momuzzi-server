package org.depromeet.team3.surveycategory

import jakarta.persistence.*
import org.depromeet.team3.common.BaseTimeEntity

@Entity
@Table(
    name = "tb_survey_category_master",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_survey_category_name_parent",
            columnNames = ["name", "parent_id"]
        ),
        UniqueConstraint(
            name = "uk_survey_category_order_parent", 
            columnNames = ["sort_order", "parent_id"]
        )
    ]
)
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

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,

    @Column(name = "is_deleted", nullable = false)
    val isDeleted: Boolean = false
) : BaseTimeEntity()
