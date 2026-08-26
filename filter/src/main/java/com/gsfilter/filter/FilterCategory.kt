package com.gsfilter.filter

import androidx.annotation.StringRes

data class FilterCategory(
    val id: String,
    @StringRes val nameRes: Int = 0,
    val name: String? = null,
)
