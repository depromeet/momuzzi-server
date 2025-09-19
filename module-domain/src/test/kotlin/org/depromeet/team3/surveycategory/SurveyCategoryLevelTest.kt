package org.depromeet.team3.surveycategory

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SurveyCategoryLevelTest {

    @Test
    fun `SurveyCategoryLevel enum 값들이 올바르게 정의되어 있다`() {
        // when & then
        assertNotNull(SurveyCategoryLevel.BRANCH)
        assertNotNull(SurveyCategoryLevel.LEAF)
    }

    @Test
    fun `SurveyCategoryLevel enum 값들의 이름이 올바르다`() {
        // when & then
        assertEquals("BRANCH", SurveyCategoryLevel.BRANCH.name)
        assertEquals("LEAF", SurveyCategoryLevel.LEAF.name)
    }

    @Test
    fun `SurveyCategoryLevel enum 값들의 순서가 올바르다`() {
        // when & then
        assertEquals(0, SurveyCategoryLevel.BRANCH.ordinal)
        assertEquals(1, SurveyCategoryLevel.LEAF.ordinal)
    }

    @Test
    fun `SurveyCategoryLevel enum 값들의 개수가 올바르다`() {
        // when
        val values = SurveyCategoryLevel.values()

        // then
        assertEquals(2, values.size)
    }

    @Test
    fun `BRANCH는 상위 레벨을 의미한다`() {
        // when
        val level = SurveyCategoryLevel.BRANCH

        // then
        assertEquals("BRANCH", level.name)
        assertEquals(0, level.ordinal)
    }

    @Test
    fun `LEAF는 하위 레벨을 의미한다`() {
        // when
        val level = SurveyCategoryLevel.LEAF

        // then
        assertEquals("LEAF", level.name)
        assertEquals(1, level.ordinal)
    }
}
