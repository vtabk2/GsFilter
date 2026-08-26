package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterPackJsonTest {

    @Test
    fun `json pack creates filters and clamps unsafe values`() {
        val pack = FilterPackJson.parse(
            """
            {
              "defaultCategoryId": "cinematic",
              "categories": [
                {"id": "cinematic", "name": "Cinematic"}
              ],
              "filters": [
                {
                  "id": "teal",
                  "name": "Teal",
                  "categoryIds": ["cinematic"],
                  "recipe": {
                    "effect": "ink",
                    "redShift": 180,
                    "adjustments": {
                      "contrast": 180,
                      "sharpness": -20
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val filter = pack.filtersForCategory("cinematic").single()

        assertEquals("cinematic", pack.defaultCategory.id)
        assertEquals("Teal", filter.name)
        assertEquals(FilterEffect.Ink, filter.recipe.effect)
        assertEquals(100, filter.recipe.redShift)
        assertEquals(100, filter.recipe.adjustments.contrast)
        assertEquals(0, filter.recipe.adjustments.sharpness)
    }

    @Test
    fun `json pack rejects unknown category ids`() {
        val result = runCatching {
            FilterPackJson.parse(
                """
                {
                  "categories": [
                    {"id": "cinematic", "name": "Cinematic"}
                  ],
                  "filters": [
                    {"id": "teal", "name": "Teal", "categoryIds": ["missing"]}
                  ]
                }
                """.trimIndent(),
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
