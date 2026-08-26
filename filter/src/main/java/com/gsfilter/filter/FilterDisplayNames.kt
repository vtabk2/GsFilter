package com.gsfilter.filter

import android.content.Context

internal fun FilterCategory.displayName(context: Context): CharSequence =
    name ?: if (nameRes != 0) context.getText(nameRes) else id

internal fun FilterOption.displayName(context: Context): CharSequence =
    name ?: if (nameRes != 0) context.getText(nameRes) else id
