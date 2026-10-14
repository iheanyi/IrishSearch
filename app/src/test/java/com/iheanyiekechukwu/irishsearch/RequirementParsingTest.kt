package com.iheanyiekechukwu.irishsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RequirementParsingTest {
    @Test fun structuresTheReportedNlpPrerequisiteWithoutLosingItsAlternative() {
        val source = "CSE 20312 and (CSE 30124 or 44648)."
        assertEquals(listOf("CSE 20312", "CSE 30124", "CSE 44648"),
            courseReferences(source, setOf("CSE")).map { it.code })
        assertEquals(ParsedCourseRequirement(CourseRequirementExpression.AllOf(listOf(
            CourseRequirementExpression.Course("CSE 20312"),
            CourseRequirementExpression.OneOf(listOf(CourseRequirementExpression.Course("CSE 30124"),
                CourseRequirementExpression.Course("CSE 44648"))),
        )), emptyList()), parseCourseRequirement(source, setOf("CSE")))
    }

    @Test fun structuredTrailingOrRetainsCourseMarkersAndFootnoteMeaning() {
        assertEquals(ParsedCourseRequirement(CourseRequirementExpression.OneOf(listOf(
            CourseRequirementExpression.Course("EG 10117", true),
            CourseRequirementExpression.Course("MATH 10850", true),
        )), listOf("* May be taken concurrently.")), parseCourseRequirement(
            "EG 10117* or MATH 10850*.\n* May be taken concurrently.", setOf("EG", "MATH")))
    }

    @Test fun uncertainCommaListsMixedOperatorsAndProseKeepSourcePresentation() {
        listOf("CSE 10001, 10101 or MATH 10850", "CSE 10001 or 10101 and MATH 10850",
            "CSE 10001 and department approval", "(CSE 10001 or 10101", "CSE 10001 or")
            .forEach { assertNull(it, parseCourseRequirement(it, setOf("CSE", "MATH"))) }
    }

    @Test fun linksInheritedCodeInTheReportedPrerequisiteList() {
        val source = "EG 10118, CSE 10001, 10101, ITAO 20210, ACMS 20220, MATH 10860, " +
            "CDT 30010, EG 10117* or MATH 10850*. * May be taken concurrently."
        val references = courseReferences(source, setOf("EG", "CSE", "ITAO", "ACMS", "MATH", "CDT"))
        assertEquals(listOf("EG 10118", "CSE 10001", "CSE 10101", "ITAO 20210", "ACMS 20220",
            "MATH 10860", "CDT 30010", "EG 10117", "MATH 10850"), references.map { it.code })
        assertEquals("10101", source.substring(references[2].range))
        assertEquals(" or ", source.substring(references[7].range.last + 2, references[8].range.first))
    }

    @Test fun linksCompactAlternativesWithoutMovingParenthesesOrOperators() {
        val source = "(CSE 20311, 20133 or 20232) and MATH 20630"
        assertEquals(listOf("CSE 20311", "CSE 20133", "CSE 20232", "MATH 20630"),
            courseReferences(source, setOf("CSE", "MATH")).map { it.code })
    }

    @Test fun finalOrAlternativeRetainsTheConcurrentAsterisk() {
        val source = "CSE 10001 or 10101*\n* May be taken concurrently."
        val references = courseReferences(source, setOf("CSE"))
        assertEquals(listOf("CSE 10001", "CSE 10101"), references.map { it.code })
        assertEquals(" or ", source.substring(references[0].range.last + 1, references[1].range.first))
        assertEquals('*', source[references[1].range.last + 1])
    }

    @Test fun renderedLinesKeepInheritedLinksAcrossHtmlBreaksAndPreserveFootnotes() {
        val source = PathRequirements.fromHtml("""
            <p class="prereq">CSE 10001 or<br/>10101<sup>*</sup><br/>
            <sup>*</sup> May be taken concurrently.</p>
        """).prerequisites
        assertNull(parseCourseRequirement(source, setOf("CSE")))
        val lines = courseReferenceLines(source, setOf("CSE"))
        assertEquals(listOf("CSE 10001 or", "10101*", "* May be taken concurrently."), lines.map { it.text })
        assertEquals(listOf(listOf("CSE 10001"), listOf("CSE 10101"), emptyList<String>()),
            lines.map { line -> line.references.map { it.code } })
        assertEquals("10101", lines[1].text.substring(lines[1].references.single().range))
        assertEquals('*', lines[1].text[lines[1].references.single().range.last + 1])
    }

    @Test fun renderedLinesResetInheritedSubjectsAtBlankLinesProseAndFootnotes() {
        val source = "CSE 10001 or\n10101\n\n20232\nCSE 20311 or\nDepartment approval required.\n20133\n" +
            "CSE 30124 or\n* May be taken concurrently.\n44648"
        val lines = courseReferenceLines(source, setOf("CSE"))
        assertEquals(source.lines().filter { it.isNotBlank() }, lines.map { it.text })
        assertEquals(listOf("CSE 10001", "CSE 10101", "CSE 20311", "CSE 30124"),
            lines.flatMap { line -> line.references.map { it.code } })
    }

    @Test fun exclusionChipsUseTheActualCse10101CourseOnlyList() {
        val source = "ACMS 24216\nCDT 30010\nITAO 30210"
        val lines = courseReferenceLines(source, setOf("ACMS", "CDT", "ITAO"))
        assertEquals(listOf("ACMS 24216", "CDT 30010", "ITAO 30210"), plainCourseListCodes(lines))
        assertEquals(source.lines(), lines.map { it.text })
    }

    @Test fun exclusionOperatorsGroupingProseAndNotesKeepLiteralPresentation() {
        val sources = listOf("CSE 10001 or\n10101*", "(CSE 10001 and 10101) or MATH 10850",
            "Cannot have received credit for CSE 10001", "CSE 10001*\n* May be taken concurrently.")
        sources.forEach { source ->
            val lines = courseReferenceLines(source, setOf("CSE", "MATH"))
            assertNull(source, plainCourseListCodes(lines))
            assertEquals(source.lines(), lines.map { it.text })
        }
        assertEquals("CSE 10101", courseReferenceLines(sources.first(), setOf("CSE"))[1].references.single().code)
    }

    @Test fun inheritanceRestoresTheOuterSubjectAfterParentheses() {
        val source = "CSE 10001 or (MATH 10850 and 10860) or 10101"
        assertEquals(listOf("CSE 10001", "MATH 10850", "MATH 10860", "CSE 10101"),
            courseReferences(source, setOf("CSE", "MATH")).map { it.code })
    }

    @Test fun unknownSubjectsProseRangesAndStandaloneNumbersDoNotInventLinks() {
        val source = "10101; CSE 10001, FAKE 12345, 10101. CSE 20311 requires a grade of 20133; " +
            "CSE 20232-20311; MATH 10850 10860"
        assertEquals(listOf("CSE 10001", "CSE 20311", "CSE 20232", "MATH 10850"),
            courseReferences(source, setOf("CSE", "MATH")).map { it.code })
    }

    @Test fun retainsTheExplicitFootnoteLineBreakFromUniversityHtml() {
        val requirements = PathRequirements.fromHtml("""
            <p class="prereq">Prerequisites: EG 10117<sup>*</sup> or MATH 10850<sup>*</sup>.<br/>
            <sup>*</sup> May be taken concurrently.</p>
        """)
        assertEquals("Prerequisites: EG 10117* or MATH 10850*.\n* May be taken concurrently.", requirements.prerequisites)
    }

    @Test fun removesRepeatedProgramsWithoutChangingTheFinalOrAlternative() {
        val requirements = PathRequirements.fromHtml("""
            <p class="cls">Enrollment limited to students with a semester level of Freshman, Sophomore, Junior or Senior.</p>
            <p class="maj">Enrollment is limited to students with a program in Advanced Physics, Chemical Engineering,
            Computer Engineering, Computer Science, Computer Science, Computer Science, Mathematics or Physics.</p>
        """)
        assertEquals("Enrollment limited to students with a semester level of Freshman, Sophomore, Junior or Senior.\n" +
            "Enrollment is limited to students with a program in Advanced Physics, Chemical Engineering, " +
            "Computer Engineering, Computer Science, Mathematics or Physics.", requirements.enrollmentRestrictions)
    }

    @Test fun preservesUnknownRestrictionWordingAndDistinctParagraphs() {
        val requirements = PathRequirements.fromHtml("<p>Must obtain instructor approval.</p><p>Only available on campus.</p>",
            restrictionDepartment = "<p>Must obtain instructor approval.</p>")
        assertEquals("Must obtain instructor approval.\nOnly available on campus.", requirements.enrollmentRestrictions)
    }
}
