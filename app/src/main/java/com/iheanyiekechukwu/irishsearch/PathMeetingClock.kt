package com.iheanyiekechukwu.irishsearch

/** PATH publishes morning times as three digits (for example 925 for 9:25 AM). */
internal fun parsePathClock(raw: String): Int? {
    val digits = raw.trim()
    if (!digits.matches(Regex("\\d{3,4}"))) return null
    val padded = digits.padStart(4, '0')
    val hour = padded.take(2).toInt()
    val minute = padded.takeLast(2).toInt()
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}
