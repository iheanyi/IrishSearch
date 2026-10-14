package com.iheanyiekechukwu.irishsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoursePlanningTest {
    @Test fun parsesPathMorningAndAfternoonClocks() {
        assertEquals(9 * 60 + 25, parsePathClock("925"))
        assertEquals(16 * 60 + 30, parsePathClock("1630"))
        assertNull(parsePathClock("2460"))
    }

    @Test fun linksExplicitAndInheritedCodesWithCatalogSubjects() {
        val rule = "(CSE 20311, 20133 or 20232) and MATH 20630; FAKE 12345"
        assertEquals(
            listOf("CSE 20311", "CSE 20133", "CSE 20232", "MATH 20630"),
            courseReferences(rule, setOf("CSE", "MATH")).map { it.code },
        )
    }

    @Test fun parsesFixedAndVariableCredits() {
        assertEquals(CreditHours(3.0, 3.0), parseCreditHours("3 Hours"))
        assertEquals(CreditHours(1.0, 3.0), parseCreditHours("1–3 Credit Hours"))
        assertNull(parseCreditHours("Variable credit"))
    }

    @Test fun totalsUniqueCoursesAndMarksUnknownCredits() {
        val classes = listOf(saved("CSE 20311", "3 Hours", "1"),
            saved("CSE 20311", "3 Hours", "2"),
            saved("MATH 20630", "1-2 Hours", "3"),
            saved("CSE 21312", "", "4"))
        assertEquals("4–5 known credits · 1 class pending", creditTotal(classes))
    }

    @Test fun availabilityRequiresEveryMeetingToFitSelectedDaysAndWindows() {
        assertEquals(false, MeetingPreferences.acceptsMeeting(2, 660, 735,
            setOf(0), setOf("morning", "midday")))
        assertEquals(false, MeetingPreferences.acceptsMeeting(0, 660, 735,
            setOf(0, 2), setOf("morning")))
        assertEquals(true, MeetingPreferences.acceptsMeeting(0, 660, 735,
            setOf(0, 2), setOf("morning", "midday")))
    }

    private fun saved(code: String, credits: String, crn: String) = SavedClass(
        schoolId = "nd",
        semesterCode = "202610",
        semesterLabel = "Fall 2026",
        section = CourseSection("", code, code, "01", crn, "", "", "", "", "", ""),
        creditHours = credits,
    )
}
