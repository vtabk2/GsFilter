package com.gsfilter.filter

import org.json.JSONArray
import org.json.JSONObject

object FilterPackJson {

    fun parse(json: String): FilterPack {
        val root = JSONObject(json)
        val categories = root.getJSONArray("categories").mapObjects { item ->
            FilterCategory(
                id = item.getString("id"),
                name = item.optString("name").takeIf { it.isNotBlank() },
            )
        }
        val categoryIds = categories.map { it.id }.toSet()
        val filters = root.getJSONArray("filters").mapObjects { item ->
            val filterCategoryIds = item.categoryIds()
            require(filterCategoryIds.isNotEmpty()) {
                "Filter '${item.getString("id")}' must define categoryIds."
            }
            require(filterCategoryIds.all { it in categoryIds }) {
                "Filter '${item.getString("id")}' references an unknown category."
            }
            FilterOption(
                id = item.getString("id"),
                categoryIds = filterCategoryIds,
                name = item.optString("name").takeIf { it.isNotBlank() },
                recipe = item.optJSONObject("recipe")?.recipe() ?: FilterRecipe(),
            )
        }
        val defaultCategoryId = root.optString("defaultCategoryId").takeIf { it.isNotBlank() }
        val defaultCategory = defaultCategoryId?.let { id ->
            categories.firstOrNull { it.id == id }
        } ?: categories.first()

        return FilterPack(
            categories = categories,
            options = filters,
            defaultCategory = defaultCategory,
            defaultFilter = FilterCatalog.default,
        )
    }

    private fun JSONObject.recipe(): FilterRecipe =
        FilterRecipe(
            isMonochrome = optBoolean("isMonochrome", false),
            redShift = optInt("redShift", 0).coerceIn(COLOR_SHIFT_MIN, COLOR_SHIFT_MAX),
            greenShift = optInt("greenShift", 0).coerceIn(COLOR_SHIFT_MIN, COLOR_SHIFT_MAX),
            blueShift = optInt("blueShift", 0).coerceIn(COLOR_SHIFT_MIN, COLOR_SHIFT_MAX),
            adjustments = optJSONObject("adjustments")?.adjustments() ?: Adjustments(),
        )

    private fun JSONObject.adjustments(): Adjustments {
        var adjustments = Adjustments()
        ADJUSTMENT_KEYS.forEach { (control, key) ->
            if (has(key)) {
                adjustments = control.update(adjustments, optInt(key))
            }
        }
        return adjustments
    }

    private fun JSONObject.categoryIds(): Set<String> {
        val categoryIds = optJSONArray("categoryIds")
        if (categoryIds != null) {
            return categoryIds.strings().toSet()
        }
        return optString("categoryId").takeIf { it.isNotBlank() }?.let(::setOf).orEmpty()
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONArray.strings(): List<String> =
        List(length()) { index -> getString(index) }

    private val ADJUSTMENT_KEYS = mapOf(
        AdjustControl.Brightness to "brightness",
        AdjustControl.Exposure to "exposure",
        AdjustControl.Contrast to "contrast",
        AdjustControl.Highlights to "highlights",
        AdjustControl.Shadows to "shadows",
        AdjustControl.Saturation to "saturation",
        AdjustControl.Vibrance to "vibrance",
        AdjustControl.Temperature to "temperature",
        AdjustControl.Tint to "tint",
        AdjustControl.Sharpness to "sharpness",
        AdjustControl.Clarity to "clarity",
        AdjustControl.Fade to "fade",
        AdjustControl.Vignette to "vignette",
        AdjustControl.Grain to "grain",
    )

    private const val COLOR_SHIFT_MIN = -100
    private const val COLOR_SHIFT_MAX = 100
}
