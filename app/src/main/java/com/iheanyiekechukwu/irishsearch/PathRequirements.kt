package com.iheanyiekechukwu.irishsearch

import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Locale

data class CourseRequirements(
    val prerequisites: String,
    val enrollmentRestrictions: String,
    val corequisites: String,
    val cannotHaveTaken: String,
    val requiredSections: String,
    val notes: String,
)

internal data class EnrollmentRestrictionRow(val label: String?, val text: String)

object PathRequirements {
    fun from(response: JSONObject): CourseRequirements = fromHtml(
        response.optString("registration_restrictions"),
        response.optString("restriction_dep"),
        response.optString("corequisites"),
        response.optString("cannot_have_taken"),
        response.optString("required_sections"),
        response.optString("clssnotes"),
    )

    internal fun fromHtml(
        registrationRestrictions: String,
        restrictionDepartment: String = "",
        corequisites: String = "",
        cannotHaveTaken: String = "",
        requiredSections: String = "",
        notes: String = "",
    ): CourseRequirements {
        val restrictions = Jsoup.parse(registrationRestrictions)
        val prerequisiteElements = restrictions.select(".prereq")
            .filter { element -> element.parents().none { it.hasClass("prereq") } }
        val otherDocument = restrictions.clone().apply { select(".prereq").remove() }
        val otherRestrictions = elementText(otherDocument.body())
        val departmentRestrictions = plainText(restrictionDepartment)
        return CourseRequirements(
            prerequisites = prerequisiteElements.joinToString("\n") { elementText(it) },
            enrollmentRestrictions = listOf(otherRestrictions, departmentRestrictions)
                .flatMap { it.lines() }.filter { it.isNotBlank() }
                .map(::deduplicateProgramList).distinctBy { it.lowercase(Locale.ROOT) }.joinToString("\n"),
            corequisites = plainText(corequisites),
            cannotHaveTaken = plainText(cannotHaveTaken),
            requiredSections = plainText(requiredSections),
            notes = plainText(notes),
        )
    }

    private fun plainText(html: String): String = elementText(Jsoup.parse(html).body())

    /** Preserves actual paragraph and BR boundaries, including superscript footnote markers. */
    private fun elementText(element: Element): String {
        val copy = element.clone()
        val boundary = "\uE000"
        copy.select("br").forEach { it.before(boundary) }
        copy.select("p,li,div").forEach { it.appendText(boundary) }
        return copy.wholeText().split(boundary).map { it.replace(Regex("[\\s\u00A0]+"), " ").trim() }
            .filter { it.isNotBlank() }.joinToString("\n")
    }

    /** Only the verified program-list template is normalized; unknown restrictions remain literal. */
    private fun deduplicateProgramList(line: String): String {
        val match = Regex("^(Enrollment(?: is)? limited to students with a program in )(.+?)(\\.)?$",
            RegexOption.IGNORE_CASE).matchEntire(line) ?: return line
        val values = match.groupValues[2].split(',').map { it.trim() }
        val unique = values.distinctBy { it.lowercase(Locale.ROOT) }
        return match.groupValues[1] + unique.joinToString(", ") + match.groupValues[3]
    }

    internal fun enrollmentRows(text: String): List<EnrollmentRestrictionRow> = text.lines()
        .filter { it.isNotBlank() }.map { line ->
            val match = Regex("^Enrollment(?: is)? limited to students with a (semester level of|program in) (.+)$",
                RegexOption.IGNORE_CASE).matchEntire(line)
            if (match == null) EnrollmentRestrictionRow(null, line)
            else EnrollmentRestrictionRow(
                if (match.groupValues[1].startsWith("semester", ignoreCase = true)) "Year levels" else "Programs",
                match.groupValues[2].removeSuffix("."),
            )
        }
}
