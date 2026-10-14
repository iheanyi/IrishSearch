# Optional Google sync for IrishSearch schedules

IrishSearch currently stores saved classes and named schedules on the phone. A Google integration should be optional and should keep the local library usable offline.

## Two distinct features

1. **Back up and sync saved plans across phones.** Use Google Drive's hidden `appDataFolder` with the `drive.appdata` OAuth scope. This scope is non-sensitive and limits the app to its own hidden data. Store a versioned JSON document containing classes, plans, stable IDs, update timestamps, and deletion tombstones. On sign-in, merge remote and local items by ID and last edit, then upload the merged document with Drive revision/ETag checks to avoid lost updates. Let users disconnect and delete cloud data.
2. **Project a chosen plan into Google Calendar.** Request Calendar authorization only after the user chooses this action. Use the narrow `calendar.app.created` scope to create a secondary calendar for that plan and create recurring events for its class meetings between the section start/end dates. Keep Google calendar IDs and event IDs per plan/meeting so later edits update or delete only IrishSearch-created items. Alternative schedules should have separate calendars so users can hide them independently. Do not silently put every tentative plan on the user's primary calendar.

For a lightweight one-off export, Android's `CalendarContract.ACTION_INSERT` intent can prefill one class event without `WRITE_CALENDAR` permission; the user chooses a calendar and saves it. This is not cross-device plan sync and does not support reliable bulk updates.

## Required setup before implementation

- A Google Cloud project with Drive API and Calendar API enabled.
- Android OAuth client for `com.iheanyiekechukwu.irishsearch` with the Play app signing SHA-1 and a debug client for development.
- OAuth consent branding, verified app information, and a public privacy policy URL. Request scopes at the point of use through Google Identity Services `AuthorizationClient`, not at app launch.
- A data migration from the current local `SavedLibrary` JSON, plus tests for duplicate imports, concurrent edits, deleted plans, recurrence dates/time zone, and revoked authorization.
- Updated in-app privacy text and Play Data safety answers before publishing sync.

Do not put client secrets or access tokens in Git. Google may require consent-screen verification for the Calendar scope; check the final scope classification in Cloud Console.

Sources: [Drive app data](https://developers.google.com/workspace/drive/api/guides/appdata), [Calendar scopes](https://developers.google.com/workspace/calendar/api/auth), [Calendar event insertion](https://developers.google.com/workspace/calendar/api/v3/reference/events/insert), [Android authorization](https://developer.android.com/identity/authorization), [Calendar insert intent](https://developer.android.com/identity/providers/calendar-provider).
