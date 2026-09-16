package com.thirdpartyinspector.util

import java.time.Instant
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    fun toIso(ts: Long): String = Instant.ofEpochMilli(ts).toString()
}
