package org.depromeet.team3.survey_category

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SurveyCategoryTypeTest {

    @Test
    fun `SurveyCategoryType enum 값들이 올바르게 정의되어 있다`() {
        // when & then
        assertNotNull(SurveyCategoryType.CUISINE)
        assertNotNull(SurveyCategoryType.AVOID_INGREDIENT)
        assertNotNull(SurveyCategoryType.AVOID_MENU)
    }

    @Test
    fun `SurveyCategoryType enum 값들의 이름이 올바르다`() {
        // when & then
        assertEquals("CUISINE", SurveyCategoryType.CUISINE.name)
        assertEquals("AVOID_INGREDIENT", SurveyCategoryType.AVOID_INGREDIENT.name)
        assertEquals("AVOID_MENU", SurveyCategoryType.AVOID_MENU.name)
    }

    @Test
    fun `SurveyCategoryType enum 값들의 순서가 올바르다`() {
        // when & then
        assertEquals(0, SurveyCategoryType.CUISINE.ordinal)
        assertEquals(1, SurveyCategoryType.AVOID_INGREDIENT.ordinal)
        assertEquals(2, SurveyCategoryType.AVOID_MENU.ordinal)
    }

    @Test
    fun `SurveyCategoryType enum 값들의 개수가 올바르다`() {
        // when
        val values = SurveyCategoryType.values()

        // then
        assertEquals(3, values.size)
    }
}
