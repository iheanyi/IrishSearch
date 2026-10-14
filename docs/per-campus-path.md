# Per-campus PATH apps: research and distribution notes

Last checked: October 13, 2026

## Direction

Keep one shared Android codebase and a distinct build and Play listing for each campus that merits an app. The current `IrishSearch` application ID and listing remain Notre Dame only. Campus selection should be a build-time configuration, not a picker in the released app. Do not create new campus listings from this note alone; each needs its own identity, privacy text, live acceptance test, and release decision.

There is a Play policy constraint to review before making several listings. Google's [Repetitive Content policy](https://support.google.com/googleplay/android-developer/answer/18258653?hl=en) lists multiple apps with highly similar functionality, content, and user experience as a possible violation and suggests aggregation when each has little content. Campus-specific data and branding may not, by themselves, be enough to distinguish listings. Google's [Impersonation policy](https://support.google.com/googleplay/android-developer/answer/9888374?hl=en) also prohibits implying university authorization that does not exist. Keep independent-app language and avoid official campus marks unless permission is obtained.

## Verified public searches

These sites exposed public term and subject filters, accepted a read-only `fose` search request, and returned section details without a student login during this check. Availability and response fields can change.

| Campus | Public PATH site | Example term code | Verified example |
| --- | --- | --- | --- |
| Notre Dame | <https://classsearch.nd.edu/> | `202610` (Fall 2026) | Existing app source; live PATH filters and search verified |
| Brown University | <https://cab.brown.edu/> | `202610` (Fall 2026) | Public browser search and section details; `CSCI` FoSe search and `AFRI 0090` detail POST with an Android browser user agent |
| University of Louisville | <https://courseleafpath.louisville.edu/> | `4272` (Spring 2027) | `PSYC 301` search and detail response |
| Saint Louis University | <https://courses.slu.edu/> | `202710` (Fall 2026) | `CSCI` search and `CSCI 1010` section detail; [SLU registrar describes Courses@SLU](https://www.slu.edu/registrar/register/index.php) |
| University of Connecticut | <https://classes.uconn.edu/> | `1273` (Spring 2027) | `CSE 4701` search and detail response |

The four candidate sites beyond Notre Dame use the same public PATH `api/?page=fose&route=search` and `route=details` pattern, but their term codes and detail fields vary. Louisville and UConn provide `camp_html`; SLU provides `campus_code` and `hours`; Brown provides `credits_html` and other campus-specific fields. Seat text can say `Seats Avail` or `Seats Available`. Public instructor names differ by school and section. Treat these as adapter variants with live checks, not proof that every CourseLeaf customer has the same API.

Brown has a transport caveat. Its AWS WAF returned HTTP 202 with `x-amzn-waf-action: challenge` for the app's `IrishSearch/3.0` user agent, while a standard Android Chrome user agent received HTTP 200 for the page and public search and detail API calls. The difference was reproducible without browser cookies. Before enabling a Brown build, use the installed Android browser's current user agent rather than a hard-coded Chrome version, test on a device, and confirm the university continues to permit these read-only requests.

## Not ready for an app configuration

- Penn's PATH experience requires PennKey authentication for the relevant student flow; no compatible public section search was verified.
- Tulane's [PATH case study](https://www.courseleaf.com/case-studies/) concerns first-year preregistration. Its public class schedule uses a different system, so the current FoSe adapter does not apply.

The [CourseLeaf case studies](https://www.courseleaf.com/case-studies/) identify institutions and products, but a case study is not evidence that a campus has a public term-specific PATH search. Recheck each university's registrar guidance and public site before enabling it.

## Build and release shape

When a second campus app is justified, add Gradle product flavors for campus-specific application IDs, app names, URLs, privacy policies, and assets. Keep PATH parsing, saved schedules, and seat checks in shared code. Set the campus in build configuration so a released app makes requests only to its campus. Keep Notre Dame's existing application ID and signing lineage for its Play update; each new campus listing needs a unique application ID and its own release metadata. Run one search, one detail lookup, local save/schedule, and seat parsing on an emulator against each configured campus before publishing.
