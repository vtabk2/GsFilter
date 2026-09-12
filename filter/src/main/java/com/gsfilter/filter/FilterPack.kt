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

    private val filtersByCategory: Map<String, List<FilterOption>> by lazy {
        categories.associate { category ->
            category.id to options.filter { category.id in it.categoryIds }
        }
    }
    private val filtersById: Map<String, FilterOption> by lazy {
        options.associateBy { it.id } + (defaultFilter.id to defaultFilter)
    }

    fun filtersForCategory(categoryId: String): List<FilterOption> =
        filtersByCategory[categoryId].orEmpty()

    fun categoryForFilter(filter: FilterOption): FilterCategory? =
        categories.firstOrNull { it.id in filter.categoryIds }

    fun categoryById(id: String): FilterCategory? =
        categories.firstOrNull { it.id == id }

    fun filterById(id: String): FilterOption? =
        filtersById[id]
}
