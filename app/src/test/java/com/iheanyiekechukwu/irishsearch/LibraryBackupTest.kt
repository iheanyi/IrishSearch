package com.iheanyiekechukwu.irishsearch

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBackupTest {
    @Test fun roundTripsCompleteLibraryAndCommitmentsInVersionedEnvelope() {
        val library = library()
        val raw = encodeBackup(library)
        assertEquals("irishsearch-library", JSONObject(raw).getString("format"))
        assertEquals(1, JSONObject(raw).getInt("version"))
        assertEquals(library, decodeBackup(raw))
    }

    @Test fun rejectsUnsupportedVersionAndUnwrappedLegacyDocuments() {
        val envelope = JSONObject(encodeBackup(library())).put("version", 2)
        assertThrows(IllegalArgumentException::class.java) { decodeBackup(envelope.toString()) }
        assertThrows(IllegalArgumentException::class.java) { decodeBackup(LibraryRepository.encode(library())) }
        assertThrows(IllegalArgumentException::class.java) { decodeBackup("{}") }
    }

    @Test fun rejectsMalformedJsonLenientSyntaxDuplicateKeysAndTrailingData() {
        val valid = encodeBackup(library())
        listOf(
            valid.dropLast(3),
            valid + "{}",
            valid.replace("\"format\":", "format:"),
            valid.replace("\"irishsearch-library\"", "'irishsearch-library'"),
            "{\"format\":\"irishsearch-library\",\"version\":1,\"version\":1,\"library\":{\"classes\":[],\"plans\":[]}}",
            valid.replace("\"plans\":", "/* comment */\"plans\":"),
        ).forEach { raw ->
            assertThrows("Must reject $raw", IllegalArgumentException::class.java) { decodeBackup(raw) }
        }
    }

    @Test fun rejectsWrongPrimitiveTypesMissingRecordsAndDuplicateIds() {
        val valid = encodeBackup(library())
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.put("version", "1") },
            { it.getJSONObject("library").getJSONArray("classes").getJSONObject(0).put("watchSeats", "true") },
            { it.getJSONObject("library").getJSONArray("classes").getJSONObject(0).put("lastSeats", 1.5) },
            { it.getJSONObject("library").getJSONArray("classes").getJSONObject(0).remove("section") },
            { it.getJSONObject("library").getJSONArray("classes").apply { put(getJSONObject(0)) } },
            { it.getJSONObject("library").getJSONArray("plans").apply { put(getJSONObject(0)) } },
        )
        mutations.forEach { mutation ->
            val envelope = JSONObject(valid).apply(mutation)
            assertThrows(IllegalArgumentException::class.java) { decodeBackup(envelope.toString()) }
        }
    }

    @Test fun rejectsMissingAndCrossSemesterReferences() {
        val valid = encodeBackup(library())
        val missing = JSONObject(valid)
        missing.getJSONObject("library").getJSONArray("plans").getJSONObject(0)
            .getJSONArray("classIds").put(0, "notre-dame:202610:missing")
        assertThrows(IllegalArgumentException::class.java) { decodeBackup(missing.toString()) }
        val otherTerm = JSONObject(valid)
        otherTerm.getJSONObject("library").getJSONArray("plans").getJSONObject(0).put("semesterCode", "202710")
        assertThrows(IllegalArgumentException::class.java) { decodeBackup(otherTerm.toString()) }
    }

    @Test fun rejectsInvalidCommitmentDaysTimesAndDates() {
        val valid = encodeBackup(library())
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.getJSONArray("days").put(0, 7) },
            { it.getJSONArray("days").put(1, 0) },
            { it.put("startMinute", 1200).put("endMinute", 900) },
            { it.put("starts", "2026-02-30") },
            { it.put("ends", "2026-01-01") },
            { it.put("starts", "2026-08-25").put("ends", "2026-08-25") },
        )
        mutations.forEach { mutation ->
            val envelope = JSONObject(valid)
            val commitment = envelope.getJSONObject("library").getJSONArray("plans")
                .getJSONObject(0).getJSONArray("commitments").getJSONObject(0)
            mutation(commitment)
            assertThrows(IllegalArgumentException::class.java) { decodeBackup(envelope.toString()) }
        }
    }

    @Test fun rejectsOversizedInputBeforeParsing() {
        assertThrows(IllegalArgumentException::class.java) { decodeBackup(" ".repeat(MaxBackupBytes + 1)) }
        assertThrows(IllegalArgumentException::class.java) { decodeBackup("é".repeat(MaxBackupBytes / 2 + 1)) }
    }

    @Test fun importKeepsExistingRecordsDisablesNewAlertsAndIsIdempotent() {
        val existing = library()
        val changed = existing.classes.single().copy(
            creditHours = "5 Hours", watchSeats = false, lastSeats = 99,
            section = existing.classes.single().section.copy(title = "Changed title"),
        )
        val added = saved("20002").copy(watchSeats = true)
        val incoming = SavedLibrary(
            classes = listOf(changed, added),
            plans = listOf(existing.plans.single().copy(name = "Changed schedule", classIds = setOf(added.id)),
                existing.plans.single().copy(id = "second-plan", name = "Alternate schedule", classIds = setOf(added.id))),
        )
        val merged = mergeBackup(existing, decodeBackup(encodeBackup(incoming)))
        assertEquals(existing.classes.single(), merged.classes.first { it.id == changed.id })
        assertEquals(existing.plans.single(), merged.plans.first { it.id == "plan" })
        assertFalse(merged.classes.first { it.id == added.id }.watchSeats)
        assertTrue(merged.classes.first { it.id == changed.id }.watchSeats)
        assertEquals(2, merged.classes.size)
        assertEquals(2, merged.plans.size)
        assertEquals(merged, mergeBackup(merged, incoming))
    }

    @Test fun rejectsMergedLibraryThatWouldNoLongerFitInABackup() {
        val largeMeetingMetadata = "x".repeat(110_000)
        fun largeLibrary(prefix: String) = SavedLibrary(classes = (1..10).map { index ->
            val saved = saved("$prefix$index")
            saved.copy(section = saved.section.copy(meetingTimes = largeMeetingMetadata))
        })
        val existing = largeLibrary("old")
        val incoming = largeLibrary("new")
        // Each input independently fits the actual envelope, not merely a class-count budget.
        assertEquals(existing, decodeBackup(encodeBackup(existing)))
        assertEquals(incoming, decodeBackup(encodeBackup(incoming)))
        assertThrows(IllegalArgumentException::class.java) { mergeBackup(existing, incoming) }
        assertEquals(10, existing.classes.size)
        assertEquals(existing, decodeBackup(encodeBackup(existing)))
    }

    private fun library(): SavedLibrary {
        val saved = saved("20001")
        return SavedLibrary(listOf(saved), listOf(SchedulePlan("plan", "Fall schedule", "notre-dame", "202610",
            "Fall 2026", setOf(saved.id), listOf(PersonalCommitment("work", "Campus job", setOf(0, 2),
                900, 1020, "2026-08-24", "2026-12-11")))))
    }

    private fun saved(crn: String) = SavedClass("notre-dame", "202610", "Fall 2026",
        CourseSection("key", "CSE 20311", "Fundamentals", "01", crn, "Mon 9:00–9:50 AM", "Professor",
            "Open", "2026-08-24", "2026-12-11", "[]"),
        "3 Hours", 4, true, 1_790_000_000_000L, false)
}
