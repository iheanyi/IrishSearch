# Student planning UX review

Reviewed October 13, 2026 using the Kotlin/Compose source and a USB-connected Pixel 8 Pro (Husky). The working scenario is a Notre Dame student finding Fall or Spring classes, checking requirements, comparing sections, and assembling alternate semester plans before registration.

## Journeys and changes

| Student decision | Observed friction | Implemented response |
| --- | --- | --- |
| Find viable sections | Provider times such as `925` were rejected and appeared as TBA. This also hid morning classes from the calendar and time filters. | Share a PATH time parser across presentation, filtering, and calendar planning; accept both three- and four-digit times. |
| Read requirements | Related courses need a section choice and a route back to the original class. Raw `A`/`F` status codes are not understandable. | Same-semester related-section sheet, readable statuses, and section navigation history. |
| Evaluate a class | The floating Add action could cover the final requirement or textbook link. A preview blamed the candidate for conflicts already in a plan. | Reserve bottom space, deduplicate the preview candidate, and distinguish existing-plan conflicts from candidate conflicts. |
| Build a week | Overlapping grid events hide one another. Classes without valid times disappear from the grid. | Default agenda with exact times, tappable section details, named overlap warnings, and a separate TBA group; retain the spatial week view. |
| Compare options | Plans show only a count and credits. Rebuilding an alternate plan duplicates work. | Semester-grouped plans with overlap, unknown-time, and free-weekday summaries; duplicate a plan and immediately open its copy. |
| Choose sections | Course titles alone do not distinguish two sections. Switches suggest settings rather than selection. | Checkbox rows include section numbers and full meeting times, with the full row as the touch target. |
| Correct mistakes | Plan deletion is immediate; removing a saved class also removes it from every plan. | Confirm plan deletion and offer Undo for saved-class removal. |
| Register | Local planning needs a useful next step without implying enrollment. | Copy the selected CRNs and explain that registration and eligibility are confirmed in NOVO. No external registration link. |
| Watch for seats | Notifications have no tap destination; only the first 20 watches are checked. | Stable saved-class notification intent, all watched sections checked, cancellation preserved, and a disabled watch rechecked before sending an alert. |

## Hierarchy and component choices

Search keeps semester and subject sheet actions visible as Material 3 outlined buttons, with a separate filter action. Detailed filtering lives in a bottom sheet. Course detail prioritizes meeting times, credits, and current enrollment, with requirements below. Adding a section remains the primary floating action; saving is a separate bookmark action.

Schedules use native secondary Agenda/Week tabs. The agenda is the readable default for narrow screens and large text. Week view supports spatial comparison. Conflicts have both text and color; missing times never imply that a schedule is conflict-free. Semester grouping, credit totals, and plan names establish the comparison context before the student opens a plan.

The review used the design and Android development skills. Native Material 3 behavior and minimum touch targets take precedence over the design skill's web-specific CSS examples.

## Concrete inspiration

- [Outlook agenda](https://mobbin.com/screens/3a21d1a0-0bec-472b-9c07-8a33dddf45cf): time and event information aligned beneath clear day headings.
- [Tiimo daily plan](https://mobbin.com/screens/8ac4c7ed-88ce-4579-9eeb-ddfbee4ea458): short, readable activity groups and clear day context.
- [Toggl timeline](https://mobbin.com/screens/54ed66ae-9358-4255-bc84-e39c0031546b): time-proportional event blocks as a secondary spatial view.
- [Structured agenda](https://mobbin.com/screens/26fae4e6-2163-4d93-8c47-5bd9839cb369): fresh redesign reference for aligned time gutters and compact event groups.
- [Amie schedule](https://mobbin.com/screens/18d0ae58-327f-4eed-9e90-de3dd079b250): concise event identity in a spatial calendar.
- [Jobber schedule](https://mobbin.com/screens/29dca97e-075f-4b8d-b1d3-fe1164a764d2): clear day context and functional timeline hierarchy.
- [Android bottom-sheet guidance](https://developer.android.com/develop/ui/compose/components/bottom-sheets) and [adaptive list-detail guidance](https://developer.android.com/develop/adaptive-apps/guides/list-detail) informed native component and navigation choices.

## Device walkthrough evidence

On Husky, searching CSE 20311 in Fall 2026 returned Fundamentals of Computing section 01. Its detail showed 4 credits, 21 seats open, and 54 enrolled of a maximum of 75 at the time of inspection. Following the CSE 21311 corequisite opened two lab sections in the same semester. Section 02 showed Monday 5:00–5:50 PM and 0 credits. Creating `UX Fall A` from this lab, returning to the original lecture, and adding that lecture produced two sections and 4 total credits. Back returned from the related lab to the original lecture.

That walkthrough exposed the morning-time parser bug: the lecture initially displayed TBA even though PATH published MWF 9:25–10:15 AM. The parser fix was verified on Husky: lecture meetings appeared Monday, Wednesday, and Friday 9:25–10:15 AM. The original plan and an independently edited copy, CRN clipboard contents, and cold-start notification navigation were verified before the visual redesign; these checks are distinguished from the subsequent visual acceptance below.

## Boundaries

Public PATH omits instructor and room information for the inspected sections. Do not infer those values. Corequisite checks indicate whether explicit course codes appear in a plan; they do not evaluate every eligibility rule. Partial or malformed meeting records can make conflict checking incomplete, so summaries say “No known time conflicts.” Saved schedules remain local planning aids, and seat checks depend on connectivity and Android background scheduling.

## Material 3 implementation contract

The app uses Compose Material 3, its built-in components and Android animation APIs. The requested `pick-ui-library` skill covers web libraries; adding one would not improve a native Compose app. The requested `mobile-native` and Emil skills inform interaction quality, while Android guidance governs platform behavior. The `find-animation-opportunities` section below is a separate read-only audit.

| Token | Implementation | Usage |
| --- | --- | --- |
| Color | `IrishSearchTheme.kt` paired light/dark `ColorScheme` roles | Neutral surfaces, blue selection containers, readable on-container text; error meaning uses text as well as color |
| Type | Native `Typography()` | Screen title `headlineSmall`24; plan/class title `titleLarge`22; row/group `titleMedium`16; content `bodyMedium`14; metadata `bodySmall`12 |
| Shape | Theme small12, medium16, large24dp | Native control defaults; medium agenda/plan surfaces, small day columns |
| Spacing | `AppSpacing.Detail/Inline/Row/Group/Page/Section/Expanded` = 4/8/12/16/16/24/32dp | Shared implementation in browse, detail, planner, agenda, registration; compact page16dp, group16dp, row12dp, inline8dp |
| Touch | Native M3 controls;48dp minimum for actions | Whole-row selection and semantic labels; compact calendar has agenda as the fully readable alternative |
| Motion | Compose springs and short state transitions | Native sheets/dialogs/ripple;140ms agenda/week content fade; short requirement disclosure; no decorative result entrances |

## Visual review

| Before | After | Why |
| --- | --- | --- |
| Large repeated app/page headings | One destination heading; compact plan title with Back and overflow | More useful course content visible immediately |
| Strong gold selected surfaces and inconsistent metadata colors | Paired ink-blue and neutral Material roles in both themes | Consistent state hierarchy and contrast |
| Browse filters and subject cards used unrelated widths and baselines | Aligned semester/subject controls; wrapping day chips and full-width time choices; fixed Apply footer | Meeting filters scan as a coherent selection form |
| Large repeated detail cards | Compact meeting/credit/seat summary, restrained outlined Preview | Meetings and availability lead; Add stays primary |
| Duplicate corequisite code plus separate status | One linked code-and-status token, expression connectors preserved | Status becomes useful navigation without repeating information |
| Three competing planner actions | Classes action plus Duplicate/Delete in overflow | Clear primary task and safer destructive action |
| Large outlined agenda cards | Left time gutter and compact tonal event row | Fast comparison of start/end times with class identity |
| Five-day week clipped offscreen at normal scale | Single-lane compact week fits five weekdays; overlaps/large text can scroll with an explicit cue | Maintains spatial comparison without hiding ordinary weekdays |
| Free weekdays described as missing published data | “No classes Tue & Thu” or “No known classes” when incomplete | Distinguishes free time from missing meeting data |

## Read-only animation opportunities audit

This section proposes remaining opportunities; it does not implement them or re-review existing animations. Native duration scale must remain authoritative, including instant transitions when the system disables animations. Input-triggered keyboard changes should remain immediate.

| # | Location | Today | Purpose | Frequency | Suggested motion and function gate |
| --- | --- | --- | --- | --- | --- |
| 1 | `MainActivity.kt`, SavedPage keyed rows | Removing/restoring a saved class can move adjacent rows immediately | Spatial consistency | Occasional | `Modifier.animateItem` placement using `spring(dampingRatio=1f, stiffness=1500f)` with a120ms alpha fade for the affected row. Retain immediate removal/Undo state. Helps track the changed item; never animate the whole list entrance. |
| 2 | `RegistrationHandoff.kt`, copy confirmation label | “Copy section IDs” changes directly to “Section IDs copied” | Feedback | Occasional | Optional120ms `Crossfade` alpha with native `FastOutSlowInEasing`; fixed button width/height prevents layout motion. Confirmation is clearer without delaying the clipboard action. |

Rejected candidates:

- Browse search results stagger — rejected by frequency and function: students repeatedly scan and act on dense data; entries should become immediately usable.
- Calendar event fly-in/reposition animation on every plan choice — rejected by function: the student needs stable time geometry to compare plans.
- Credit-count odometer — rejected by function: a changing number briefly obscures the value used to choose a viable course load.
- Keyboard search/selection scale or entrance — rejected by frequency/input gate: keyboard-driven actions need immediate response.

Verdict: this planning app needs little additional motion. Native sheets, ripple, predictive back, and restrained state changes already cover the major transitions. The highest-value remaining opportunity is tracking an individually removed/restored saved row. Treat the table as a future implementation handoff, not a reason to delay functional verification.

## Validation ledger

| Area | Evidence | Result / limit |
| --- | --- | --- |
| PATH meeting parsing | Husky live CSE20311; shared parser tests | Verified MWF9:25–10:15 AM; three-digit `925` no longer becomes TBA |
| Student corequisite journey | Husky pre-redesign live lecture→same-term lab→Back | Lab02 Monday5–5:50PM,0 credits; original lecture4 credits; both in UX Fall A |
| Planner edits | Husky pre-redesign original plus copy | Original2 sections/4 credits; edited copy1 lab/0 credits; delete confirmation inspected |
| CRN clipboard | Husky pre-redesign paste into own search field without submitting | Actual value `12835, 12832` verified, field cleared |
| Notification tap route | Husky pre-redesign force-stop then OPEN_SAVED_CLASS intent | Saved lab detail opens after library load; actual WorkManager delivery untested |
| Redesigned browse | Husky dark screenshot `/tmp/irish-redesign-browse-dark.png` | Single heading, aligned controls and neutral selected colors inspected |
| Redesigned agenda | Husky dark screenshot `/tmp/irish-redesign-agenda-dark.png` | Compact header, time gutter, four readable meeting rows, registration action visible; screenshot predates user-requested secondary tabs |
| User-reported scroll jitter | Exact bottom repro: `/private/tmp/irish-husky-subject-bottom.mp4`; contact sheet `/tmp/irish-subject-bottom-contact.png` | Confirmed severe whole-sheet vertical oscillation at final WR subject; title enters status-bar region and Apply footer shifts.3607 frames,19 current-deadline janky(0.53%),1933 legacy(53.59%); fixed and retested as described below |
| Build/static checks | Coordinating agent final combined build | assembleDebug, 36 unit tests (0 failures), lintDebug (0 errors, 4 maintenance warnings), and bundleRelease pass, including the final cross-line prerequisite-link fix. |
| Light/dark/large text | Husky `/tmp/irish-week-light.png`, `/tmp/irish-week-light-2x.png`, `/tmp/irish-week-dark-tabs.png` | Week verified in both themes and at2× text; large text uses explicit sideways-scroll cue. Original night=yes,font_scale=1.0 restored. Other final surfaces still pending |

The subject-sheet oscillation gate passed after the fixed-height implementation. Final dark-theme Add/Create/requirement and calendar checks passed as recorded below. Final light-theme coverage remains incomplete because the bedtime appearance override prevented the requested theme change. Runtime TalkBack speech, actual background notification delivery, and an API24 device run are not claimed.

## Severity-ranked findings and acceptance

| Severity | Surface | Finding | Implementation / acceptance evidence |
| --- | --- | --- | --- |
| P1 | Subject selector | At bottom, repeated flings cause the entire sheet to oscillate vertically, including fixed header and Apply. Frame-deadline metrics alone understate this visible layout defect. | Fixed: stable height computed before dialog measurement, gesture handoff disabled for Subject, explicit Close. Exact twelve-fling retest reached WR with identical title bounds before/after; reverse fling also stable. |
| P2 | Planner navigation | Checkmarked segmented buttons communicate selection rather than related content views. | Replaced with `SecondaryTabRow` and two native `Tab` children; Husky dark/light screenshots verify no selection checkmarks. |
| P2 | Agenda accessibility | Focused class surface omitted the time gutter from its announcement. | Added time-range contentDescription with merged descendants; actual spoken TalkBack untested. |
| P2 | Detail requirements | Provider abbreviations can leave bare course numbers unlinked; repeated restriction names impair scanning. | CSE 40657 renders All of with nested One of; inherited CSE 44648 opens two same-term sections. Cross-line inherited-subject parsing also has two regression tests in the final build. |
| P2 | Large-text layouts | Fixed-width time gutter could wrap times; related sheet header could consume results space. | Gutter now scales with font scale; related-sheet owner adjusting adaptive content. Final font-size inspection pending. |

### Subject-bottom regression retest

Installed intermediate build: version3.0/code10, targetSdk36, device update2026-10-13 21:40:38. This build includes the structural sheet fix but predates the subsequent tonal-surface/helper-copy and Add/Create refinements.

- Exact twelve rapid flings reached final `WR — Writing and Rhetoric` row. Header bounds were identical before and after: `[60,748][1140,844]`. The final row and footer remain separate and fully visible: `/tmp/irish-subject-fixed-bottom.png`.
- Video: `/private/tmp/irish-husky-subject-fixed.mp4`; reverse fling video: `/private/tmp/irish-husky-subject-fixed-reverse.mp4`. Both retain fixed header/footer geometry.
- First pass after install:29/757 current-deadline janky frames(3.83%),p95=22ms,p99=34ms. Warmed pass:7/818(0.86%),p95=13ms,p99=34ms. These are debug-build/device observations, not a claim of zero jank or a controlled benchmark. The severe layout oscillation is visibly resolved independently of these metrics.
- Native week tabs and compact calendar accepted in dark/light: five weekdays fit at normal scale; all three fixture classes remain visible at correct times. At2× font, horizontal scrolling is explicitly indicated and text remains legible.

### Schedule-flow correction

Native screenshots exposed three inconsistent flows: `/tmp/irish-create-before-1x.png`, `/tmp/irish-add-detail-before-1x.png`, `/tmp/irish-add-detail-before-2x.png`, and the separate Saved membership editor `/tmp/irish-add-dialog-before-1x.png`.

Creation now uses a concise name/semester form with labeled full-width controls, one Create schedule action, keyboard Done, and scrollable content. The new plan opens immediately into a checkbox class chooser; an empty chosen semester offers Find classes and preserves plan context on return. Detail Add uses a separate single-destination selection flow; Saved membership editing uses checkboxes because a section can belong to several plans. Final installed creation, class selection, calendar navigation, and QA cleanup passed at normal text size; see the October 14 ledger.

[Android dialog guidance](https://developer.android.com/develop/ui/compose/components/dialog) supports native dialogs for concise user input and explicit confirmation/dismissal actions; native components still require deliberate hierarchy, content, and responsive layout.

## Phone spacing and layout compatibility: evidence table

Material 3 compatibility is not a single certification flag. Compose provides color, typography, shape, and component tokens; the app defines its own spacing scale. The final compact page gutter is16dp, matching official Android phone layout guidance; the earlier20dp gutter was replaced during this audit. The implementation uses native M3 components and needs rendered acceptance at each target size.

| Area | Implemented rule | Verified on Husky | Remaining limit |
| --- | --- | --- | --- |
| Spacing scale | `AppSpacing`4/8/12/16/24/32dp; inline8, row12, group16, compact page16 | Browse controls, detail summary, agenda, week, and fixed subject screenshot show aligned edges | Numerical dimensions such as icon24, target48, agenda time gutter72, and calendar geometry are purposeful dimensions outside the gap scale |
| Phone grid | Compact layout below600dp;16dp horizontal gutter; expanded layouts use32dp; content widths constrained where appropriate |448dp-wide Husky: Browse controls share page edges; week fits five ordinary single-lane weekdays |600dp/tablet and landscape visual acceptance not completed; this is a phone audit, not a claim of full adaptive coverage |
| Type and component layout | Native `Typography()`, paired color roles, theme shapes; native button/chip/checkbox/radio/tab metrics | Week dark/light1× and light2× verified; old Add dialog2× captured specifically to identify problems | Final Add selection and new mode inspected at2× before the pause. Standalone Create, requirements, Subject and filters lack complete final2× coverage; no additional font changes were made after the user requested normal size |
| Agenda/week | Agenda time gutter scales with font scale; grid columns grow when text grows; sideways-scroll cue appears if needed | Week1× includes all five days;2× displays legible enlarged labels with explicit scroll cue | Agenda2× attempted while user moved to launcher, so that capture is not app evidence; final normal calendar scaling measured48dp event targets; final2× calendar scaling not repeated |
| Insets and edges | Root Scaffold handles system bars; native modal sheet insets; detail reserves104dp for FAB reach; fixed subject footer separates final row | Week content/nav and normal detail respect system bars; subject WR final row fully visible after bottom flings | Final IME-open Add/Create passed at1×; short-height/landscape sheets remain untested |
| Sheet stability | Height measured from calling window before dialog; Subject disables gesture handoff and offers48dp Close; header/footer fixed | Twelve bottom flings and reverse fling preserve exact title bounds; no former whole-sheet oscillation | Final tonal/80%height sheet passed exact WR-bottom/reverse retest; frame metrics remain separate from geometry evidence |
| Interaction size | Native controls and explicit48dp minimum actions; class selection rows at least64–72dp | Normal-sized controls visually inspected; semantics expose labels and selected states | Calendar scaling now reserves48dp after the event gap, capped at200dp/hour; meetings shorter than15minutes use72dp-minimum list rows. Nine focused calendar tests passed, including50-minute and15-minute geometry plus the sub15-minute fallback; installed50-minute fixture measured48dp;15-minute and shorter cases have focused unit coverage only |

Status: the spacing system is coherent with native Material 3 conventions, with intentional app-level choices. Full rendered compliance across every state is not yet claimed; the listed final-build checks and device gaps remain explicit.

The compact margin follows [Android content structure guidance](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure). Final Browse, Subject, detail, and Week captures verify the16dp compact gutter. Earlier light/2×Week captures use the preceding20dp layout and do not prove every final state.

Calendar geometry verification: `WeeklyCalendarLayoutTest` passed9 tests on the final source; the new checks cover all durations15–180minutes, asserting48dp minimum after the2dp gap and a200dp/hour base ceiling. Meetings under15minutes use a separate72dp-minimum list. Runtime layout at increased font scale is still checked separately.


## Final Husky acceptance: October 13–14, 2026

Device: Pixel 8 Pro (Husky), 448dp portrait width. Version 3.0 / code 10, minSdk24, targetSdk36. Final icon APK installed October14 at00:22:57; the final parser-fix APK replaced it at00:30:27. All resumed checks used font scale1.0. Original `night=yes` was restored and verified; bedtime and Do Not Disturb were not modified.

| Journey / view | Observed result | Evidence |
| --- | --- | --- |
| Final subject sheet | Twelve rapid flings reached WR, then reverse fling; title and footer stayed fixed. Header `[48,928][1152,1024]` and footer `[584,2782][958,2842]` were identical before/after; final WR fully visible. Tonal surface and16dp edges inspected. | `/private/tmp/irish-final-subject.mp4`, `/private/tmp/irish-final-subject-bottom.png` |
| Final subject performance | 14/1051 current-deadline janky frames (1.33%), p95=14ms, p99=24ms. Debug recording observation, not a controlled benchmark or zero-jank claim. | `/private/tmp/irish-final-subject-gfx-after.txt` |
| Browse expanded sections | CSE21311 shows both sections, Full/Open text, comfortable inner bottom padding plus outer divider gap. Submit dismissed keyboard. | `/private/tmp/irish-final-browse-expanded-dark.png` |
| Add destination selection | Plan radios show current→result credits or Already in this schedule. Selection and separate New mode fit1× and2× before the pause. | `/private/tmp/irish-final-add-dark.png`, `/private/tmp/irish-final-add-dark-2x.png`, `/private/tmp/irish-final-add-new-2x.png` |
| Add creation with keyboard | Name and Create and add remain fully above keyboard. Created QA Oct14 Add with CSE40657 and3 credits; keyboard/sheet dismissed and success snackbar appeared. | `/private/tmp/irish-resume-add-ime-dark.png` |
| Standalone creation | Name and labeled semester selector fit above keyboard. Opening semester selection and returning retains typed name. Create immediately selects the new plan and opens class chooser; lecture+lab gives2 sections/4 credits after Done. | `/private/tmp/irish-resume-create-ime-dark.png`, `/private/tmp/irish-resume-chooser-light.png` (filename says light, pixels are dark; see limitation below) |
| Structured prerequisites | CSE40657 shows CSE20312 AND (CSE30124 OR CSE44648) as explicit nested All of/One of groups; no duplicate/bare code. | `/private/tmp/irish-resume-prereq-dark.png` |
| Linked alternative and return | CSE44648 opens Fall2026 related sheet with2 open Data Science sections (CRNs18421,21416), TBA times. Close works; opening section01 then system Back returns to CSE40657. | `/private/tmp/irish-resume-related-dark.png`, `/private/tmp/irish-resume-related-return.png` |
| Final Week | Five weekdays fit. Lecture9:25–10:15 and lab17:00–17:50 align to the grid; event bounds are144px tall at3px/dp =48dp. Tapping lecture opens details; Back returns to plan. | `/private/tmp/irish-resume-week.png` |
| Installed launcher icon | Native launcher shows recognizable pale open-book and lens on ink-blue adaptive background. Actual themed/monochrome launcher mode was not enabled; XML and mask previews cover that asset only. | `/private/tmp/irish-resume-launcher.png`, `/private/tmp/irish-icon-review/icon-preview-final.png` |
| QA cleanup | Deleted only QA Oct14 Add and QA Oct14 Create, then removed CSE40657 saved by this test. Original UX Fall A / copy and ACCT20100, CSE21311, CSE20311 saved records remain. | `/private/tmp/irish-resume-clean-plans.png`, `/private/tmp/irish-clean-saved.xml` |

### Explicit remaining limits

- Final light-theme attempt was overridden by active bedtime appearance: `mNightMode=no` but `mAttentionModeThemeOverlay=1001` and `mComputedNightMode=true`. The supported shell interface has no attention-overlay reset/restore command. No bedtime schedule, DND, or undocumented service call was changed. Files named `irish-resume-create-light.png` and `irish-resume-chooser-light.png` show dark pixels and are **not** light-mode evidence.
- Earlier valid light and2×Week captures remain historical evidence; final standalone Create/requirements/filter2× and final light surfaces are not fully accepted. User-requested font1.0 was preserved after resuming.
- Runtime TalkBack speech, actual WorkManager notification delivery, API24 device behavior, tablet/landscape layout, live15-minute event fixture, and enabled monochrome launcher presentation were not tested.
- The earlier attempted `irish-final-add-new-ime-2x.png` captured another app after user interaction; it is excluded from app QA evidence. The successful keyboard-open check is the October14 normal-font capture above.


Final parser-build smoke: after the00:30:27 install, CSE40657 still renders the same nested All of/One of structure. Tapping CSE44648 loads the same two Fall2026 sections; system Back dismisses the sheet. Evidence: `/private/tmp/irish-final-parser-prereq.png`, `/private/tmp/irish-final-parser-related.png`, and `/private/tmp/irish-final-parser-related.xml`. Final font scale1.0 and night=yes verified. Device released after this narrow smoke; no production files changed during device QA.


## Cannot-have-taken follow-up: October 14, 2026

The user's live example was CSE 10101 section 01, Elements of Computing I, Fall 2026, CRN 12427. Its exclusion list is ACMS 24216, CDT 30010, and ITAO 30210. Design reference: the restrained grouping in [Starling eligibility](https://mobbin.com/screens/353728f1-c56a-4096-9f8b-7312006f6efb), adapted to native Material 3 roles and controls.

| Before | After | Why |
| --- | --- | --- |
| Three detached underlined lines under Cannot have taken, unlike neighboring structured requirements | One neutral tonal panel with wrapping outlined course chips, each 48dp tall, and consistent inset/gaps | Groups exclusions clearly and gives each linked course an obvious touch target without implying positive eligibility or unsupported completion status |

The implementation chips only lists made entirely of course references. Prose and operator expressions retain their linked source wording inside the panel. It does not introduce All of/One of or pass/fail labels for exclusions.

Scoped Husky acceptance passed on the APK installed at **2026-10-14 08:03:29**, with font scale **1.0** and night mode **yes** unchanged:

- Expanded panel shows all three chips; collapse/re-expand works. UI bounds measure 144px at 3px/dp = 48dp per chip.
- ACMS 24216 opens the Fall 2026 related-course sheet. The catalog returns zero sections and the sheet explicitly says No sections found for Fall Semester 2026. System Back returns to the source course.
- Enrollment restrictions still expands independently, showing Programs: Data Science and the original Main campus restriction.
- At scroll bottom the panel is fully above the Add to schedule FAB with visible separation. FAB opens the correct CSE 10101 Add sheet; Back dismisses it. No records were created or changed.
- Evidence: `/private/tmp/irish-exclusion-before.png`, `/private/tmp/irish-exclusion-after.png`, `/private/tmp/irish-exclusion-related.png`, `/private/tmp/irish-exclusion-neighbor-bottom.png`, and matching XML files.

This incremental build passed assembleDebug, **38 unit tests (0 failures)**, and lintDebug per the coordinating agent. The earlier 36-test combined build remains the historical result for the preceding parser fix. No additional light/large-text claims are made for this scoped follow-up.
