package com.gsfilter.filter

import android.net.Uri

object FilterSourceKey {

    fun asset(path: String): String = "asset:$path"

    fun withRevision(sourceKey: String, revision: Long): String = "$sourceKey:revision=$revision"

    fun file(path: String, length: Long, lastModifiedMillis: Long): String =
        "file:$path:$length:$lastModifiedMillis"

    fun uri(uri: Uri, width: Int, height: Int, lastModifiedMillis: Long? = null): String =
        buildString {
            append("uri:")
            append(uri)
            append(':')
            append(width)
            append('x')
            append(height)
            lastModifiedMillis?.let {
                append(':')
                append(it)
            }
        }
}
