package com.iheanyiekechukwu.irishsearch

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal const val MaxBackupBytes = 2 * 1024 * 1024
private const val BackupFormat = "irishsearch-library"
private const val BackupVersion = 1
private const val MaxClasses = 2_000
private const val MaxPlans = 200
private const val MaxCommitments = 200

/** A portable envelope; DataStore's older, tolerant format remains unchanged. */
internal fun encodeBackup(library: SavedLibrary): String {
    val result = JSONObject()
        .put("format", BackupFormat)
        .put("version", BackupVersion)
        .put("library", JSONObject(LibraryRepository.encode(library)))
        .toString()
    // Validate exports too, so an invalid local reference never produces a misleading backup.
    decodeBackup(result)
    return result
}

/** Reject the entire document before passing anything to the legacy tolerant decoder. */
internal fun decodeBackup(raw: String): SavedLibrary {
    require(raw.length <= MaxBackupBytes && raw.toByteArray(Charsets.UTF_8).size <= MaxBackupBytes) {
        "The backup is too large. Choose a file smaller than 2 MB."
    }
    try {
        StrictBackupJson(raw).validate()
        val envelope = JSONObject(raw)
        require(envelope.requiredString("format", 64) == BackupFormat) {
            "This file is not an IrishSearch library backup."
        }
        require(envelope.requiredLong("version") == BackupVersion.toLong()) {
            "This backup version is not supported."
        }
        val library = envelope.requiredObject("library")
        validateBackupLibrary(library)
        return LibraryRepository.decode(library.toString())
    } catch (error: IllegalArgumentException) {
        throw error
    } catch (error: Exception) {
        throw IllegalArgumentException("The backup is invalid or incomplete.", error)
    }
}

/**
 * Existing records win every ID collision, including their seat watches and plan contents.
 * New classes are imported with seat watches off. Repeating an import has no further effect.
 */
internal fun mergeBackup(current: SavedLibrary, incoming: SavedLibrary): SavedLibrary {
    // The API is also safe for callers that obtained a model without decodeBackup.
    validateBackupLibrary(JSONObject(LibraryRepository.encode(incoming)))
    val existingClassIds = current.classes.mapTo(mutableSetOf()) { it.id }
    val existingPlanIds = current.plans.mapTo(mutableSetOf()) { it.id }
    val classes = current.classes + incoming.classes.filter { existingClassIds.add(it.id) }
        .map { it.copy(watchSeats = false) }
    val classesById = classes.associateBy { it.id }
    val plans = current.plans + incoming.plans.filter { existingPlanIds.add(it.id) }.map { plan ->
        // Protect against a same-ID existing class belonging to another semester/campus.
        plan.copy(classIds = plan.classIds.filterTo(linkedSetOf()) { id ->
            classesById[id]?.let { it.schoolId == plan.schoolId &&
                it.semesterCode == plan.semesterCode } == true
        })
    }
    require(classes.size <= MaxClasses && plans.size <= MaxPlans) {
        "The combined library is too large to import."
    }
    val merged = current.copy(classes = classes, plans = plans)
    // Include envelope overhead and the UTF-8 byte size so a restored library stays portable.
    encodeBackup(merged)
    return merged
}

private fun validateBackupLibrary(library: JSONObject) {
    val classes = library.requiredArray("classes", MaxClasses)
    val plans = library.requiredArray("plans", MaxPlans)
    val classScopes = mutableMapOf<String, Pair<String, String>>()
    for (index in 0 until classes.length()) {
        val saved = classes.requiredObject(index)
        val school = saved.requiredIdentifier("schoolId")
        val semester = saved.requiredIdentifier("semesterCode")
        saved.requiredString("semesterLabel", 256, nonBlank = true)
        saved.requiredString("creditHours", 256)
        saved.requiredBoolean("watchSeats")
        saved.optionalBoolean("lastCheckFailed")
        saved.optionalLong("lastSeats")?.let { require(it in 0..1_000_000) { "Invalid seat count." } }
        saved.optionalLong("lastCheckedAt")?.let { require(it >= 0) { "Invalid seat check date." } }
        val section = saved.requiredObject("section")
        val crn = section.requiredIdentifier("crn")
        section.requiredString("key", 1_024)
        section.requiredString("code", 256, nonBlank = true)
        section.requiredString("title", 8_192)
        section.requiredString("section", 256)
        section.requiredString("meetingTime", 8_192)
        section.requiredString("instructor", 8_192)
        section.requiredString("status", 1_024)
        section.requiredString("starts", 64)
        section.requiredString("ends", 64)
        section.requiredString("meetingTimes", 131_072)
        require(classScopes.put("$school:$semester:$crn", school to semester) == null) {
            "The backup contains duplicate saved classes."
        }
    }
    val planIds = mutableSetOf<String>()
    for (index in 0 until plans.length()) {
        val plan = plans.requiredObject(index)
        require(planIds.add(plan.requiredIdentifier("id"))) { "The backup contains duplicate schedules." }
        plan.requiredString("name", 48, nonBlank = true)
        val school = plan.requiredIdentifier("schoolId")
        val semester = plan.requiredIdentifier("semesterCode")
        plan.requiredString("semesterLabel", 256, nonBlank = true)
        val references = plan.requiredArray("classIds", MaxClasses)
        val uniqueReferences = mutableSetOf<String>()
        for (reference in 0 until references.length()) {
            val id = references.get(reference)
            require(id is String && id.length <= 386 && uniqueReferences.add(id)) {
                "A schedule contains invalid or duplicate class references."
            }
            require(classScopes[id] == (school to semester)) {
                "A schedule references a missing class or a different semester."
            }
        }
        val commitments = plan.requiredArray("commitments", MaxCommitments)
        val commitmentIds = mutableSetOf<String>()
        for (commitmentIndex in 0 until commitments.length()) {
            val commitment = commitments.requiredObject(commitmentIndex)
            require(commitmentIds.add(commitment.requiredIdentifier("id"))) {
                "A schedule contains duplicate personal commitments."
            }
            val title = commitment.requiredString("title", 80, nonBlank = true)
            val days = commitment.requiredArray("days", 7)
            require(days.length() > 0) { "A personal commitment has no weekdays." }
            val uniqueDays = mutableSetOf<Long>()
            for (dayIndex in 0 until days.length()) {
                val day = integralBackupNumber(days.get(dayIndex))
                require(day in 0..6 && uniqueDays.add(day)) { "A personal commitment has invalid weekdays." }
            }
            val start = commitment.requiredLong("startMinute")
            val end = commitment.requiredLong("endMinute")
            require(start in 0..1439 && end in 1..1440 && start < end) {
                "A personal commitment has invalid times."
            }
            val starts = commitment.requiredDate("starts")
            val ends = commitment.requiredDate("ends")
            require(starts <= ends) { "A personal commitment ends before it starts." }
            val model = PersonalCommitment(commitment.requiredIdentifier("id"), title,
                uniqueDays.mapTo(linkedSetOf()) { it.toInt() }, start.toInt(), end.toInt(),
                starts.toString(), ends.toString())
            require(commitmentValidation(model) == null) {
                commitmentValidation(model) ?: "A personal commitment is invalid."
            }
        }
    }
}

private fun JSONObject.requiredString(key: String, max: Int, nonBlank: Boolean = false): String {
    val value = get(key)
    require(value is String && value.length <= max && (!nonBlank || value.isNotBlank())) {
        "The backup contains an invalid $key."
    }
    return value
}

private fun JSONObject.requiredIdentifier(key: String): String =
    requiredString(key, 128, nonBlank = true).also {
        require(it.none { character -> character == ':' || character.isWhitespace() || character.code < 32 }) {
            "The backup contains an invalid $key."
        }
    }

private fun JSONObject.requiredObject(key: String): JSONObject = get(key).also {
    require(it is JSONObject) { "The backup is missing $key." }
} as JSONObject

private fun JSONArray.requiredObject(index: Int): JSONObject = get(index).also {
    require(it is JSONObject) { "The backup contains an invalid record." }
} as JSONObject

private fun JSONObject.requiredArray(key: String, max: Int): JSONArray = get(key).also {
    require(it is JSONArray && it.length() <= max) { "The backup contains an invalid $key list." }
} as JSONArray

private fun JSONObject.requiredBoolean(key: String) {
    require(get(key) is Boolean) { "The backup contains an invalid $key." }
}

private fun JSONObject.optionalBoolean(key: String) {
    if (has(key)) requiredBoolean(key)
}

private fun JSONObject.requiredLong(key: String): Long = integralBackupNumber(get(key))
private fun JSONObject.optionalLong(key: String): Long? = if (has(key)) requiredLong(key) else null

private fun integralBackupNumber(value: Any): Long {
    require(value is Int || value is Long) { "The backup contains an invalid integer." }
    return (value as Number).toLong()
}

private fun JSONObject.requiredDate(key: String): LocalDate {
    val raw = requiredString(key, 10)
    require(raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "A personal commitment has an invalid date." }
    return try {
        LocalDate.parse(raw)
    } catch (error: java.time.DateTimeException) {
        throw IllegalArgumentException("A personal commitment has an invalid date.", error)
    }
}

/** Android's JSON parser accepts non-JSON syntax; scan before parsing and reject duplicate keys. */
private class StrictBackupJson(private val source: String) {
    private var offset = 0

    fun validate() {
        whitespace()
        require(peek() == '{') { "This file is not a JSON backup." }
        value(0)
        whitespace()
        require(offset == source.length) { "The backup contains extra data." }
    }

    private fun value(depth: Int) {
        require(depth <= 16) { "The backup is nested too deeply." }
        whitespace()
        when (peek()) {
            '{' -> {
                offset++
                whitespace()
                val keys = mutableSetOf<String>()
                if (consume('}')) return
                do {
                    whitespace()
                    val key = string()
                    require(keys.add(key)) { "The backup contains a duplicate JSON field." }
                    whitespace()
                    expect(':')
                    value(depth + 1)
                    whitespace()
                    if (consume('}')) return
                    expect(',')
                } while (true)
            }
            '[' -> {
                offset++
                whitespace()
                if (consume(']')) return
                do {
                    value(depth + 1)
                    whitespace()
                    if (consume(']')) return
                    expect(',')
                } while (true)
            }
            '"' -> string()
            't' -> literal("true")
            'f' -> literal("false")
            'n' -> literal("null")
            '-', in '0'..'9' -> number()
            else -> throw IllegalArgumentException("The backup is not valid JSON.")
        }
    }

    private fun string(): String {
        expect('"')
        val result = StringBuilder()
        while (offset < source.length) {
            val character = source[offset++]
            if (character == '"') return result.toString()
            require(character.code >= 32) { "The backup contains an invalid JSON string." }
            if (character != '\\') {
                result.append(character)
                continue
            }
            require(offset < source.length) { "The backup contains an incomplete JSON string." }
            when (val escape = source[offset++]) {
                '"', '\\', '/' -> result.append(escape)
                'b' -> result.append('\b')
                'f' -> result.append('\u000c')
                'n' -> result.append('\n')
                'r' -> result.append('\r')
                't' -> result.append('\t')
                'u' -> {
                    require(offset + 4 <= source.length) { "The backup contains an incomplete JSON escape." }
                    val digits = source.substring(offset, offset + 4)
                    require(digits.all { it in "0123456789abcdefABCDEF" }) { "The backup contains an invalid JSON escape." }
                    result.append(digits.toInt(16).toChar())
                    offset += 4
                }
                else -> throw IllegalArgumentException("The backup contains an invalid JSON escape.")
            }
        }
        throw IllegalArgumentException("The backup contains an incomplete JSON string.")
    }

    private fun number() {
        consume('-')
        if (!consume('0')) {
            require(peek() in '1'..'9') { "The backup contains an invalid JSON number." }
            while (peek() in '0'..'9') offset++
        }
        if (consume('.')) digits()
        if (consume('e') || consume('E')) {
            if (!consume('+')) consume('-')
            digits()
        }
    }

    private fun digits() {
        require(peek() in '0'..'9') { "The backup contains an invalid JSON number." }
        while (peek() in '0'..'9') offset++
    }

    private fun literal(text: String) {
        require(source.startsWith(text, offset)) { "The backup is not valid JSON." }
        offset += text.length
    }

    private fun whitespace() {
        while (peek() == ' ' || peek() == '\r' || peek() == '\n' || peek() == '\t') offset++
    }

    private fun peek(): Char = source.getOrNull(offset) ?: '\u0000'
    private fun consume(character: Char): Boolean = (peek() == character).also { if (it) offset++ }
    private fun expect(character: Char) { require(consume(character)) { "The backup is not valid JSON." } }
}
