# Notre Dame class data sources

Verified October 14, 2026. This app uses public class data and does not authenticate to Notre Dame systems.

## Original Android app

The Java source in Git history (`DepartmentActivity.java`) posted a form to `https://class-search.nd.edu/reg/srch/ClassSearchServlet` with `TERM=201310`, `DIVS=A`, `CAMPUS=M`, `SUBJ=<department>`, `ATTR=0ANY`, and `CREDIT=A`. `CourseActivity.java` fetched `?CRN=<number>&TERM=201310`. The `class-search.nd.edu` hostname no longer resolves, so these calls cannot be reused for current terms. The old parser did read an instructor string from its HTML result.

## Current public PATH

The app reads semester and subject options from `https://classsearch.nd.edu/`, then posts search criteria to `/api/?page=fose&route=search` and section lookups to `/api/?page=fose&route=details`. Semester `202610` was Fall 2026 at verification time. A public CSE search returned 328 sections. Its CSE 10001 section 01 response included structured `meetingTimes` for Monday and Wednesday, 2:00–3:15 PM; maximum enrollment 41; and six available seats.

PATH's page says “Login to view Instructor, Building, and Room information.” In that public CSE response, `instr`, `instructordetail_html`, and `instructorresult_html` were empty. We show “Not public” rather than guessing a faculty assignment. The same section returned “waitlist currently unavailable”; the app shows waitlist figures only when the response contains numeric values.

## Public NOVO Browse Classes

The [Registrar's Class Search page](https://registrar.nd.edu/courses-classrooms/class-search/) also links to [NOVO Browse Classes](https://bxeregprod.oit.nd.edu/StudentRegistration/ssb/registration). In a browser without signing in, Fall 2026 search results showed CSE 10001 section 01 and “6 of 41 seats remain.” Its detail panel showed enrollment actual 35, maximum 41, available 6, and waitlist capacity/actual/available all zero. Neither its visible result columns nor that detail panel exposed instructor names. This is a separate university data source; the app does not currently merge its enrollment data into PATH results.

Public data and availability can change. Recheck a live response before modifying parsers or claiming section-level instructor data is available.
