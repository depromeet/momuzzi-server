package org.depromeet.team3.surveycategory.util

import org.depromeet.team3.surveycategory.SurveyCategory
import org.depromeet.team3.surveycategory.SurveyCategoryLevel
import java.time.LocalDateTime

/**
 * SurveyCategory 관련 테스트 데이터 팩토리 클래스
 */
object SurveyCategoryTestDataFactory {

    fun createSurveyCategory(
        id: Long? = 1L,
        parentId: Long? = null,
        level: SurveyCategoryLevel = SurveyCategoryLevel.BRANCH,
        name: String = "한식",
        sortOrder: Int = 1,
        isDeleted: Boolean = false,
        createdAt: LocalDateTime? = LocalDateTime.now(),
        updatedAt: LocalDateTime? = LocalDateTime.now()
    ): SurveyCategory {
        return SurveyCategory(
            id = id,
            parentId = parentId,
            level = level,
            name = name,
            sortOrder = sortOrder,
            isDeleted = isDeleted,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun createBranchCategory(
        id: Long = 1L,
        name: String = "한식"
    ): SurveyCategory {
        return createSurveyCategory(
            id = id,
            parentId = null,
            level = SurveyCategoryLevel.BRANCH,
            name = name
        )
    }

    fun createLeafCategory(
        id: Long = 8L,
        parentId: Long = 1L,
        name: String = "밥류"
    ): SurveyCategory {
        return createSurveyCategory(
            id = id,
            parentId = parentId,
            level = SurveyCategoryLevel.LEAF,
            name = name
        )
    }
}

