# October 14, 2026 release validation

## Automated gates

- Integrated debug build, unit tests, lint and release bundle pass. A subsequent short-event PNG label correction was packaged in debug/release builds and verified against the regenerated native export (`/private/tmp/irish-new-export-final.png`): the 15-minute personal block now has a legible label.
- **73 unit tests, zero failures:** existing parser/planning coverage plus commitments, seat-check behavior, portable backup and calendar export.
- Lint: zero errors, four maintenance suggestions (Gradle version and Kotlin extension helpers). API 24 notification-settings fallback is explicitly guarded.
- The initial release bundle was unsigned. Subsequently, the upload-key-signed version 3.0 (10) passed signature verification and was accepted by Google Play; see the rollout addendum below. The Gradle signing configuration accepts all four signing environment variables or none; partial configuration fails.
- Release manifest and AAB DEX/resource scan exclude the debug receiver, debug worker, fixture CRN, fixture title, and fixture notification title.

## Real Android notification execution

The full suite ran on the isolated API 34 emulator using the first integrated debug build under the old package `com.iheanyiekechukwu.irishsearch`. The same production checker, DataStore adapter, notifier and WorkManager APIs were exercised with a debug-only availability source. A debug application prevents ordinary workers from requesting the synthetic course from PATH.

| Scenario | Observed result |
| --- | --- |
| Zero seats to one | One genuine Android notification, persisted baseline and successful timestamp |
| Unchanged count | No new alert; original notification post time unchanged |
| Decrease | No alert |
| Permission denied | Availability persisted; no notification and no crash |
| Fetch failure | Baseline and last successful timestamp retained; failure status set |
| Unwatch or removal during delayed fetch | No alert; completed with zero checked after rechecking saved state |
| Cancel during fetch | Cancellation propagated; zero baseline and no notification. The first 10-second attempt finished too early, so the valid run used a 60-second delay and recorded `worker cancelled`. |
| Tap notification in shade | Opened the actual TEST 10001 / section 01 saved detail. Its synthetic PATH-detail fetch fails as expected. |
| Cleanup | Fixture, work and notification removed; initial denied permission restored |

Evidence: `/private/tmp/irish-seat-alert-validation/validation-summary.txt`, `IRISH_SEAT_DEBUG.log`, `notification-shade.png`, `notification-tap.png`, and `release-bundle-scan.txt`.

The shade inspection caught an opaque launcher asset being used for the notification small icon. It was replaced with the transparent monochrome shamrock.

Final new-package smoke on Husky (`com.iheanyi.notredame.classsearch`): zero-to-one simulation completed `checked=1 failed=0 alerts=1`; Android reported one active notification, baseline 1 and a successful timestamp. Husky's active bedtime policy hid it from the shade, so no visible-shade delivery claim is made for this device. Sending that notification's genuine PendingIntent while the app was foreground opened TEST 10001 section 01. Bedtime/DND settings were not changed. The fixture was removed, the original denied notification permission restored, and the library again matched the pre-test three classes/two schedules exactly. Evidence: `/private/tmp/irish-husky-new-notification.log` and `/private/tmp/irish-husky-new-notification-tap.png`.

## Husky package migration

The owner chose `com.iheanyi.notredame.classsearch` after confirming the original signing key is unavailable. The new debug app was installed alongside the original; the original package was not cleared or uninstalled.

- Took a read-only copy of the old app's DataStore file and wrapped its existing library in the portable backup envelope, adding only new-field defaults.
- Used the new app's native Android file-picker restore flow. The preview showed **3 saved classes and 2 schedules**.
- Verified successful restore, both schedules and credit totals in the UI.
- Read back the new DataStore and compared it with the normalized pre-migration backup: **exactly equal**.
- Confirmed Husky font scale **1.0**, unchanged. The installed shamrock adaptive icon was inspected in Android App info. The final APK was installed at 09:24:32 on October 14; a fresh DataStore read still exactly matched the migration backup (3 classes / 2 plans).
- Evidence: `/private/tmp/irish-husky-restore-preview.png`, `/private/tmp/irish-husky-restored-schedules.png`, `/private/tmp/irish-husky-shamrock-installed.png`. Local backup remains `/private/tmp/irish-husky-migration-backup.json`; the phone copy is `Download/IrishSearch-transfer.json`.

## Native UI acceptance

The new API 34 package passed native light/dark, large-text, keyboard, commitment overlap/edit, 15-minute calendar target, backup save/restore, calendar export, and image-sharing checks. The exported calendar contains five recurring weekday events including the personal commitment, with Indianapolis time-zone transitions. See `student-ux-review.md` for exact screenshots and limits. Actual TalkBack speech, API 24, tablet/landscape and several secondary native branches remain untested; automated/source checks are reported separately.

## Google Play internal rollout addendum

On October 14, 2026, the new listing enrolled in Google-managed Play App Signing and accepted the signed 3.0 (10) bundle. After the owner approved its reusable tester list, the release was published to internal testing. Console reports **Active**, **Available to internal testers**, and **Not reviewed**. Evidence: `/private/tmp/irishsearch-internal-release-live.jpg`. The upload key and password are backed up locally and in 1Password; public certificate fingerprints and the artifact hash are recorded in `play-release.md`.

This confirms console acceptance and internal distribution only. Installation and runtime checks on Google's delivered binary remain pending. Preserve the installed debug app's library before replacing it because its signing certificate differs from the Play-signed version.
