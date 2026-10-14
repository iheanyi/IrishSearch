# Seat alert validation without registration season

The receiver and fake availability worker below are compiled only into debug APKs. The worker uses the production `SeatAlertChecker`, `LibrarySeatAlertStore`, and `SeatAlertNotifications`. Only the dedicated `notre-dame:202610:debug-seat-alert` fixture can receive fake results. Periodic and manual production workers always use live PATH responses. A debug-only Application excludes the fixture from normal workers, so enabling the fixture cannot race a live PATH check. Those workers log `normal worker excluded debug fixture` when filtering it.

Build/install using the project's normal debug workflow, then run these commands on the authorized device. `operation setup` clears only the dedicated debug fixture/work/notification and restores a watched baseline of zero. `cleanup` removes them afterward.

```sh
adb shell pm grant com.iheanyi.notredame.classsearch android.permission.POST_NOTIFICATIONS
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation setup
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ei seats 1
adb logcat -d -s IRISH_SEAT_DEBUG:I
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation inspect
```

Wait for `completed checked=1 failed=0 alerts=1`, then inspect: baseline 1, a successful timestamp, one active notification. Its title is `A seat opened in TEST 10001`. Open Android's notification shade and tap it to exercise the genuine notification `PendingIntent`; the selected saved class should be `TEST 10001 · Section 01`. The synthetic class has no real PATH detail record, so a fetch error is expected after navigation. A debug `operation tap` can also send the active notification's actual `PendingIntent` while the app is foreground.

Check the same count and then a lower count:

```sh
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ei seats 1
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation inspect
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ei seats 0
adb logcat -d -s IRISH_SEAT_DEBUG:I
```

Both completed summaries must report `alerts=0`; the unchanged-count inspect must retain the notification's original `postTime`.

Permission denial must safely record availability without posting:

```sh
adb shell pm revoke com.iheanyi.notredame.classsearch android.permission.POST_NOTIFICATIONS
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation setup
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ei seats 1
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation inspect
adb shell pm grant com.iheanyi.notredame.classsearch android.permission.POST_NOTIFICATIONS
```

Expect successful baseline 1, zero notifications, `permitted=false`, and `alerts=0`. Restoring permission does not retrospectively alert on an unchanged count.

Failure retains the baseline and last successful timestamp:

```sh
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation setup
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ei seats 1
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation inspect
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ez fail true
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation inspect
```

Wait for each completed job. The failed job reports `failed=1 alerts=0`; baseline 1 and the previous successful timestamp remain, with `failed=true`.

For an unwatch/removal/cancellation race, set up a zero baseline, enqueue a delayed check, wait for its `fetch started` log, then issue exactly one of `unwatch`, `remove`, or `cancel`:

```sh
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation setup
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation check --ei seats 1 --el delay_ms 10000
adb logcat -d -s IRISH_SEAT_DEBUG:I
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation unwatch
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation inspect
```

Run the scenario separately with `remove`, then separately with `cancel`. Unwatch/removal completes with `checked=0 alerts=0`. Cancellation logs `worker cancelled` and keeps baseline zero without marking a failure. None may post a notification.

```sh
adb shell am broadcast -n com.iheanyi.notredame.classsearch/com.iheanyiekechukwu.irishsearch.SeatAlertDebugReceiver --es operation cleanup
```

JVM regression coverage is in `SeatAlertCheckerTest`. Run it with the normal unit-test task and filter `com.iheanyiekechukwu.irishsearch.SeatAlertCheckerTest`. Release APK verification should confirm that `SeatAlertDebugReceiver` and `SeatAlertDebugWorker` are absent from the manifest and DEX.
