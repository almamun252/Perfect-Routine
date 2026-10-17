# Perfect Routine 2.4.0 — audit and acceptance checklist

Baseline: the user's 2.3.0 commit c2e1e8c, whose source corresponds to the supplied
profile UI update (authoring commit 773d0c3), plus local .gitignore and schema 4/5.
The new patch leaves those local files intact. The user reported a successful
2.3.0 debug build/unit run and 11 device tests on V2126 / Android 14.

## Confirmed problems fixed

| Area | Finding and change |
| --- | --- |
| Start time | No user-selectable start. Both creation forms now accept start/date/time; omission resolves to save time. Existing edits retain start; clearing it explicitly uses the new save time. |
| Date picker | UTC picker timestamps were interpreted directly as local dates. Calendar-day conversion now handles both positive and negative offsets and resets seconds. |
| Invalid interval | A new or changed deadline cannot precede start. Older invalid/overdue intervals can still be edited if their schedule is unchanged. |
| Quick-list order | Dragging rewrote taskDate and re-saved whole stale tasks. Separate sortOrder is updated transactionally by ID, preserving all other fields. Drag calculations follow the task's current position and accumulate movement. |
| Quick-list access | Tapping a quick task's title/note now opens details/edit. Start and deadline can also be set in quick-add. A stable save key reduces duplicate saves during rotation. |
| Profile | Replaced the large gradient header with a centered avatar/name, compact counts and simple settings cards. |
| Detail progress | Deadline progress uses startDate and guards zero/negative intervals. Both creation and start times are shown. |
| Task operations | Save/delete/status/order exceptions are surfaced through a UI message instead of escaping an unhandled launch. Editing a deleted task is rejected. |
| Timer accuracy | Actual monotonic elapsed time replaces a fixed 1000 ms per delayed tick; Pomodoro duration is capped at its remaining time and stops at zero. |
| Timer sessions | Mode changes preserve accrued duration. Switching task timers saves the old session first. Save completion is awaited before success is shown; failed writes keep session state for retry. |

## Previous issues reviewed

| Area | Current source behavior | Verification still needed |
| --- | --- | --- |
| Overdue on Home | HomeTaskFilter includes overdue pending tasks outside the selected upcoming range; lifecycle-aware time refresh is present. | Cross a deadline while app is foreground/background. |
| Widget | Pending deadlines within the next three days plus older overdue tasks; completed tasks excluded; event/periodic refresh instead of a permanent loop. | Vivo launcher refresh cadence and battery behavior. |
| Category deletion | CategoryManager removes suggestions, leaving tasks/notes intact; unchanged task edits do not recreate suggestions. | Delete, reopen and edit the same task. |
| Images | Gallery content copied into app storage; draft promotion/cleanup and shared-image reference checks present. | Add/remove multiple photos, relaunch, rotate, cancel an import. |
| Reminders | Exact alarms when permitted, WorkManager fallback, task/deadline revalidation, duplicate-delivery key, boot/time/package reconciliation. | Permission deny/allow, edit/cancel deadline, completion, reboot and OEM battery restrictions. |
| Database | Forward migration chain 2→3→4→5→6; startup error gate; no destructive fallback. | New migration tests and existing data on the actual phone. |
| Tabs | Stable IDs, editable icons/colors, hide/order/delete-transfer and centered Home retained. | Rename/reorder/delete a populated tab; inspect notes/session history. |

## Known limits — not certified as fixed

- Active timer state is still held in the ViewModel. Android process termination,
  force-stop or reboot can lose an unsaved active session; durable checkpoint and
  service-owned timer recovery need a separate implementation and device tests.
- Long background timer sessions and foreground-service lifecycle limits require
  Android-version-specific testing. Timing accuracy improvements do not prove
  background survival.
- Reminders currently fire for deadlines, not for start times. Start dates are
  planning metadata; they do not hide tasks, prevent early completion, or auto-plan.
- OS restrictions can delay fallback reminders/widget refresh. Real notification
  delivery cannot be proven from source review or migration tests.
- Version 1 database history remains unknown; no guessed destructive migration.
- Photo drafts from an interrupted picker may be cleaned up later. There is no
  new export/restore or online backup feature in this update.
- Android rendering, large fonts, screen rotation and accessibility still require
  a visual pass on the device. No claim of pixel-perfect or bug-free UI is made.

## Authoring validation

Kotlin syntax parsing, git diff checks and SQLite smoke checks are performed before
packaging. SQL checks execute the actual 4→5→6 SQL on empty/populated data and inspect
preservation of original columns, notes/image paths, sessions and order-only edits.
These are NOT Android Room validation or compiled Kotlin/JUnit execution.

New automated tests: TaskScheduleTest (local date round trips across three time
zones and interval boundaries), TaskScheduleMigrationTest (3 device tests).
The full device suite is expected to contain 14 tests if the prior suite is unchanged.
Android build/tests cannot run on the authoring machine (SDK/dependencies unavailable).

## Apply and test

1. Keep the existing 2.3.0 commit. Extract the update ZIP; run Apply-Update.ps1 from
   the folder containing it, targeting the PerfectRoutine project root. A source
   mismatch stops all writes; do not bypass its hashes.
2. Run assembleDebug, testDebugUnitTest and connectedDebugAndroidTest. Keep the
   same signing key, and do not uninstall or clear phone data. Database becomes 6;
   older DB-v5 APKs cannot open it. Source backups are not database downgrades.
3. Create in both Task list and Quick list: (a) no start selected; (b) tomorrow
   09:00 start / 10:00 end; (c) start after end, which must show an error.
4. Reopen and edit only the title: original start must remain. Change start and
   verify details show both created/start values. Clear start to use current time.
5. Drag quick tasks, relaunch and verify order, starts, deadlines and notes remain.
6. Confirm old tasks/notes/images/session history and tab names/icons persist.
7. Check Profile, keyboard scrolling, date/time cancellation, screen rotation and
   large fonts. The compact time input accepts hours/minutes directly.
8. Run a short Pomodoro to zero; pause/resume stopwatch; switch mode while paused;
   save a session and switch to another task. Verify duration is attributed to the
   correct task. Test screen-off behavior separately.
9. Exercise reminder, widget, category and photo cases in the table above.
10. Commit generated schema 6.json with the validated source. Push only when ready.
