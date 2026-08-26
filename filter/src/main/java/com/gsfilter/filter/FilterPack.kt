package com.gsfilter.filter

data class FilterPack(
    val categories: List<FilterCategory>,
    val options: List<FilterOption>,
    val defaultCategory: FilterCategory,
    val defaultFilter: FilterOption,
) {
    init {
        require(categories.isNotEmpty()) { "FilterPack requires at least one category." }
        require(options.isNotEmpty()) { "FilterPack requires at least one filter option." }
        require(categories.map { it.id }.toSet().size == categories.size) {
            "FilterPack category ids must be unique."
        }
        require(options.map { it.id }.toSet().size == options.size) {
            "FilterPack filter ids must be unique."
        }
    }

    fun filtersForCategory(categoryId: String): List<FilterOption> =
        options.filter { categoryId in it.categoryIds }

    fun categoryForFilter(filter: FilterOption): FilterCategory? =
        categories.firstOrNull { it.id in filter.categoryIds }

    fun categoryById(id: String): FilterCategory? =
        categories.firstOrNull { it.id == id }

    fun filterById(id: String): FilterOption? =
        if (id == defaultFilter.id) {
            defaultFilter
        } else {
            options.firstOrNull { it.id == id }
        }
}
