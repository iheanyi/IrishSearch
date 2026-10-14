package com.iheanyiekechukwu.irishsearch

import java.text.NumberFormat

internal data class CreditHours(val minimum: Double, val maximum: Double)

private val creditRange = Regex(
    "^\\s*(\\d+(?:\\.\\d+)?)\\s*(?:-|–|to)\\s*(\\d+(?:\\.\\d+)?)",
    RegexOption.IGNORE_CASE,
)
private val singleCredit = Regex("^\\s*(\\d+(?:\\.\\d+)?)")

internal fun parseCreditHours(raw: String): CreditHours? {
    val range = creditRange.find(raw)
    val minimum = (range?.groupValues?.get(1) ?: singleCredit.find(raw)?.groupValues?.get(1))
        ?.toDoubleOrNull() ?: return null
    val maximum = range?.groupValues?.get(2)?.toDoubleOrNull() ?: minimum
    if (minimum !in 0.0..30.0 || maximum !in minimum..30.0) return null
    return CreditHours(minimum, maximum)
}

internal fun creditTotal(classes: List<SavedClass>): String {
    val distinctCourses = classes.distinctBy { it.section.code }
    val parsed = distinctCourses.map { parseCreditHours(it.creditHours) }
    val known = parsed.filterNotNull()
    val missing = parsed.size - known.size
    if (known.isEmpty()) return if (distinctCourses.isEmpty()) "0 credits" else "Credits unavailable"
    val minimum = known.sumOf { it.minimum }
    val maximum = known.sumOf { it.maximum }
    val formatter = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }
    val amount = if (minimum == maximum) formatter.format(minimum)
        else "${formatter.format(minimum)}–${formatter.format(maximum)}"
    return if (missing == 0) "$amount credits"
        else "$amount known credits · $missing ${if (missing == 1) "class" else "classes"} pending"
}
