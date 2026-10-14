package com.iheanyiekechukwu.irishsearch

import java.util.Locale

internal data class CourseReference(val range: IntRange, val code: String)

private val courseCodeCandidate = Regex(
    "\\b(?:([A-Z]{1,8})\\s+)?(\\d{4,5}[A-Z]?)\\b",
    RegexOption.IGNORE_CASE,
)
private val courseListOperator = Regex("\\b(?:and|or)\\b", RegexOption.IGNORE_CASE)

/** Inherits a catalog subject only within a course list, respecting parenthesis scope. */
internal fun courseReferences(text: String, subjects: Set<String>): List<CourseReference> =
    buildList {
        val catalogSubjects = subjects.map { it.uppercase(Locale.ROOT) }.toSet()
        val subjectScopes = mutableListOf<String?>(null)
        var next = 0
        fun resetSubjects() {
            subjectScopes.indices.forEach { subjectScopes[it] = null }
        }
        for (match in courseCodeCandidate.findAll(text)) {
            val prefix = match.groups[1]?.value?.uppercase(Locale.ROOT)
            val isOperator = prefix == "AND" || prefix == "OR"
            val number = match.groups[2] ?: continue
            val range = if (prefix == null || isOperator) number.range else match.range
            val between = text.substring(next, range.first)
            val withoutOperators = courseListOperator.replace(between, "")
            val listSeparator = withoutOperators.all { it.isWhitespace() || it in ",()*/" } &&
                !between.contains(Regex("\\n\\s*\\n"))
            if (!listSeparator) resetSubjects()
            for (character in between) when (character) {
                '(' -> subjectScopes.add(subjectScopes.last())
                ')' -> if (subjectScopes.size > 1) subjectScopes.removeAt(subjectScopes.lastIndex)
                    else resetSubjects()
            }
            val explicitSubject = prefix?.takeIf { it in catalogSubjects && !isOperator }
            val hasListConnector = between.any { it == ',' || it == '/' || it == '\n' } ||
                courseListOperator.containsMatchIn(between)
            val subject = when {
                explicitSubject != null -> explicitSubject
                prefix != null && !isOperator -> null
                listSeparator && hasListConnector -> subjectScopes.last()
                else -> null
            }
            if (subject != null) {
                subjectScopes[subjectScopes.lastIndex] = subject
                add(CourseReference(range, "$subject ${number.value.uppercase(Locale.ROOT)}"))
            } else resetSubjects()
            next = range.last + 1
        }
    }

internal sealed interface CourseRequirementExpression {
    data class Course(val code: String, val markedWithAsterisk: Boolean = false) : CourseRequirementExpression
    data class AllOf(val children: List<CourseRequirementExpression>) : CourseRequirementExpression
    data class OneOf(val children: List<CourseRequirementExpression>) : CourseRequirementExpression
}

internal data class ParsedCourseRequirement(
    val expression: CourseRequirementExpression,
    val footnotes: List<String>,
)

/** Parses only complete, unambiguous course expressions. Commas and prose retain the source renderer. */
internal fun parseCourseRequirement(text: String, subjects: Set<String>): ParsedCourseRequirement? {
    val paragraphs = text.trim().lines().filter { it.isNotBlank() }
    if (paragraphs.isEmpty()) return null
    var source = paragraphs.first().trim()
    val notes = paragraphs.drop(1)
    if (notes.any { !it.trimStart().startsWith('*') }) return null
    val inlineNote = Regex("\\*\\s*May be taken concurrently\\.?", RegexOption.IGNORE_CASE).find(source)
    val footnotes = if (inlineNote != null) listOf(source.substring(inlineNote.range.first)) + notes else notes
    if (inlineNote != null) source = source.substring(0, inlineNote.range.first).trim()
    source = source.removeSuffix(".").trimEnd()
    val references = courseReferences(source, subjects).associateBy { it.range.first }
    if (references.isEmpty() || references.size > 30) return null
    val tokens = mutableListOf<Pair<String, String>>()
    var position = 0
    while (position < source.length) {
        if (source[position].isWhitespace()) { position++; continue }
        val reference = references[position]
        if (reference != null) {
            tokens += "course" to reference.code
            position = reference.range.last + 1
            continue
        }
        val character = source[position]
        if (character in "()*") {
            tokens += character.toString() to character.toString()
            position++
            continue
        }
        val operator = Regex("(?:and|or)\\b", RegexOption.IGNORE_CASE).find(source, position)
            ?.takeIf { it.range.first == position } ?: return null
        tokens += operator.value.lowercase(Locale.ROOT) to operator.value
        position = operator.range.last + 1
    }
    val parser = CourseExpressionParser(tokens)
    val expression = parser.parseLevel(0) ?: return null
    return if (parser.finished) ParsedCourseRequirement(expression, footnotes) else null
}

private class CourseExpressionParser(private val tokens: List<Pair<String, String>>) {
    private var position = 0
    val finished: Boolean get() = position == tokens.size

    fun parseLevel(depth: Int): CourseRequirementExpression? {
        if (depth > 12) return null
        val children = mutableListOf(parseAtom(depth) ?: return null)
        var operation: String? = null
        while (position < tokens.size && tokens[position].first in setOf("and", "or")) {
            val nextOperation = tokens[position++].first
            // English mixed operators without explicit parentheses have no proven precedence.
            if (operation != null && operation != nextOperation) return null
            operation = nextOperation
            children += parseAtom(depth) ?: return null
        }
        return when (operation) {
            "and" -> CourseRequirementExpression.AllOf(children)
            "or" -> CourseRequirementExpression.OneOf(children)
            else -> children.single()
        }
    }

    private fun parseAtom(depth: Int): CourseRequirementExpression? {
        val token = tokens.getOrNull(position++) ?: return null
        if (token.first == "(") {
            val child = parseLevel(depth + 1) ?: return null
            if (tokens.getOrNull(position++)?.first != ")") return null
            return child
        }
        if (token.first != "course") return null
        val marked = tokens.getOrNull(position)?.first == "*"
        if (marked) position++
        return CourseRequirementExpression.Course(token.second, marked)
    }
}
