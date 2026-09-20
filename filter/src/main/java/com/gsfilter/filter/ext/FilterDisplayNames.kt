package com.gsfilter.filter.ext

import android.content.Context
import com.gsfilter.filter.presets.FilterCategory
import com.gsfilter.filter.presets.FilterOption

fun FilterCategory.displayName(context: Context): CharSequence =
    name ?: if (nameRes != 0) context.getText(nameRes) else id

fun FilterOption.displayName(context: Context): CharSequence =
    name ?: if (nameRes != 0) context.getText(nameRes) else id
