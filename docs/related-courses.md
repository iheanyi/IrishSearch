# Linked prerequisite and related courses

Checked October 13, 2026 against public Notre Dame PATH Fall 2026 results. This is a UI/data contract for the course-detail work, not a claim that every registration rule can be parsed automatically.

## What PATH actually provides

`CSE 20312` section 01 (CRN 13527) rendered:

> Prerequisites: (CSE 20311, 20133 or 20232) and (CSE 20110*, MATH 20630 or 10850). *May be taken concurrently.

Its corequisite `CSE 21312` was an anchor with `data-action="result-detail"` and `data-group="code:CSE 21312"`. Clicking it opened that course's detail in the same PATH page without changing the page URL. Although its `href` was `/search/?p=CSE%2021312`, opening that URL directly returned HTTP 404. Treat the course code and current term as the link target; do not publish or depend on that href.

The app already has the same-term PATH search endpoint and exact `CourseSection.code` values. A related-course tap can search the current term for the referenced subject, filter the returned sections by **exact normalized code**, then open the detail screen. A code identifies a course, not one section: when multiple sections match, show those sections and let the student choose. If no current-term section matches, show an explicit unavailable state, with a separate prior-term search only when the student requests it. Retain the source course and back navigation.

## Presentation and parsing

- Keep the full original prerequisite/corequisite/cannot-have-taken rule visible. Logic such as `and`, `or`, parentheses, and the concurrent-enrollment `*` must not disappear when codes become tappable.
- Render a compact, labeled clickable chip for each **resolved, unambiguous** course code, such as `CSE 20311` and `MATH 20630`. A plain course-code token is not proof that a student satisfies a prerequisite; the chip only opens information about that course. Do not turn unparsed prose or broad phrases such as “department approval” into a link.
- PATH sometimes abbreviates repeated subjects in a group (`CSE 20311, 20133 or 20232`). Expand the omitted subject only when the local context is unambiguous and confirm the resulting full code exists in the selected term. If parsing is uncertain, leave the text unlinked. Deduplicate codes across repeated rules.
- Search by code through the campus's configured public catalog, not a cross-campus global ID. Cache exact resolved matches for the current term during the detail session. Preserve the rule's category (`prerequisite`, `corequisite`, `cannot have taken`) because those mean different things.
- [Material 3 chips](https://developer.android.com/develop/ui/compose/components/chip) are appropriate for a tap that opens another course. `AssistChip` has an `onClick` action; `FilterChip` implies a selected filter state and is less apt here. Provide the course code as the accessibility label and say that activation opens course details. Ensure the raw rule remains readable when chips wrap on narrow screens.

As of the [official Compose Material 3 release page](https://developer.android.com/jetpack/androidx/releases/compose-material3) checked October 13, 2026, `androidx.compose.material3:material3` stable is **1.4.0** (October 7) and **1.5.0-beta01** is beta. The Android app declares Compose BOM `2026.09.00` and receives Material 3 through that BOM in [`app/build.gradle`](../app/build.gradle). Check the BOM's resolved version before adding a direct version or adopting a beta API; a clickable chip does not require the beta.
