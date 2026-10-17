# Perfect Routine 2.4.1

Apply over the 2.4.0 Schedule Audit update. Version code 5, database version 6.
The user's latest pushed feature/profile-ui commit is 926998c; this patch checks
individual source hashes from the supplied 2.4.0 source rather than relying on
identical local Git commit IDs. No history reset/import is needed.

## Changes

1. Home/widget sort by started/not-yet-started, start timestamp ascending, priority
   High/Medium/Low, deadline and ID. Older start times rank before later ones;
   priority breaks identical-start-time ties. A future High task cannot precede
   an already-started Low task. No tasks are hidden just because start is future.
2. Existing upcoming-range and overdue inclusion rules remain. Startless legacy
   rows use taskDate. Widget eligibility remains deadline-based (next 3 days plus
   overdue); undated tasks are not newly added to the widget.
3. Add/edit form: compact font/gaps, notes/photos collapsed for new tasks and
   expanded for existing content, fixed Save below the scrolling fields. Keyboard
   and system-bar padding keep Save accessible. Large fonts/small screens can
   still require scrolling; touch targets are retained.
4. Bottom nav: selected icon slides horizontally and floats in a circular cutout.
   Home stays centered for 3/5 slots; Profile and custom tabs float when selected.
   Overflow selection and the all-tabs sheet retain an active navigation state.

## Validation

Added TaskOrderingTest and an integration case in WidgetTaskFilterTest. Before
packaging, changed Kotlin is syntax-parsed and diff/payload checks are run.
These are not an Android build or rendering test. Run on your PC:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

No new instrumentation tests are added; the existing 14-test suite from 2.4.0
should run. Keep generated schemas, .gitignore and all phone data.

## Device checks

- Create one started Low task and a future High task with deadlines within 3 days:
  started task appears first in Home and widget. Refresh widget to compare.
- Give three tasks an identical start timestamp and different priorities:
  High, Medium, Low. Verify earlier start still outranks later start.
- Open Add with/without categories, show keyboard and enter long text. Save stays
  reachable; scroll notes/photos independently. Collapse/reopen notes; data stays.
- Edit a photo/note task and confirm content is expanded and saves unchanged.
- Select Work, Home, Profile and other tabs: raised icon follows selection smoothly.
  Verify 0/1/2/3/4+ visible custom tabs, overflow picker, long labels and larger fonts.

Install through Android Studio Run using the same signing key. Do not uninstall
or downgrade the database. After checks, commit 2.4.1 on feature/profile-ui, push,
then merge main only when ready. This source package performs no GitHub writes.
