# IrishSearch

IrishSearch is an independent Android class browser and schedule planner for the University of Notre Dame. This Kotlin/Jetpack Compose project replaces the 2013 Eclipse/Ant app. Its previous Java code and assets remain available in Git history.

## Features

- Browse all public classes in a selected semester, or narrow the list by multiple subjects, keyword, allowed meeting days, and time windows. Subject and keyword can both be left empty.
- Use Notre Dame's live advanced filters for campus, course type, part of term, instructor, department, college, meeting time, instruction method, credit hours, core requirements, attributes, and class status.
- View readable meeting days and times, dates, descriptions, requirements, and an enrollment capacity chart from Notre Dame's public PATH class search. Long descriptions use **Show more**; requirement sections expand separately, and recognized course codes open matching sections in the selected semester.
- Save sections on the phone and organize them into multiple named schedules grouped by semester. Each schedule tracks known credit hours, shows a range for variable-credit courses, and flags classes whose credits are unavailable.
- Preview a class against an existing schedule before saving it, then see meeting blocks in a weekly calendar with warnings for overlapping times. On a course's detail screen, corequisite badges show whether each explicitly named course is already in the selected schedule.
- Optionally watch saved sections for seat increases. WorkManager checks watched sections periodically, requires a network and a battery that is not low, and asks for notification permission only when alerts are enabled. Tapping an alert opens that saved section. Android may delay periodic work; alerts are not immediate or guaranteed.
- Open the Notre Dame bookstore when a verified link is present in its public class details. PATH's public class details often omit course-specific book links, so the app does not claim to display a verified book list.

Notre Dame's public PATH page says instructor, building, and room details require university sign-in. The public search and detail responses we checked have empty instructor fields. Waitlist counts are displayed only if PATH supplies them; they were unavailable in the sampled sections. IrishSearch does not attempt to access private records or university accounts. Saved schedules are local planning aids and do not register anyone for classes or sync to a university system.

## Build

Open the repository root in Android Studio, use JDK 21, and install Android SDK platform 37.0. Run `./gradlew :app:assembleDebug` or `./gradlew :app:bundleRelease`.

- Application ID: `com.iheanyiekechukwu.irishsearch` (preserved from the old app)
- Version code/name: `10` / `3.0`
- Minimum Android: API 24
- Target Android: API 36
- Compile SDK: 37

The release bundle is not signed for Play until a signing configuration is provided outside Git. For an update to the existing Play listing, use its Play App Signing setup or original signing key and confirm the version code exceeds the last uploaded release.

## Play review status

The code addresses the published API level requirement and removes the old bundled AdMob, Google Analytics, Apache HTTP, and generated APK artifacts. It uses HTTPS, declares only internet and notification permissions, and includes an in-app privacy notice plus [privacy policy](PRIVACY.md).

The 2018 rejection/removal summary supplied by Play Console did not identify the specific policy. A successful build cannot establish that the app will pass review. Before submitting, inspect the current Play Console Policy status and App content pages, complete the Data safety form based on the final binary, supply a public URL for `PRIVACY.md`, and test the signed release through an internal track.

## Data source and operation

IrishSearch reads Notre Dame's public PATH class search at <https://classsearch.nd.edu/>. The site and response format are controlled by the university and may change. Search terms and section lookups are sent directly from the device. There is no IrishSearch server, account, ad SDK, or analytics SDK.

The 2013 app used `https://class-search.nd.edu/reg/srch/ClassSearchServlet` with a hard-coded `TERM=201310`; that hostname no longer resolves. [Data source notes](docs/data-sources.md) record the original request, the current public endpoints, and instructor availability checks.

Other verified public PATH campuses and a per-campus distribution plan are recorded in [campus notes](docs/per-campus-path.md). They are not enabled in this Notre Dame app.

See [PRIVACY.md](PRIVACY.md) for the user-facing policy. IrishSearch is independent and is not affiliated with or endorsed by the University of Notre Dame or CourseLeaf.
