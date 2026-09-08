kotlin
package com.thirdpartyinspector.util

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun toIso(ts: Long): String = iso.format(Date(ts))
}
