# Perfect Routine 2.2.0 — User-managed tabs

Base: c9686b8706bbc2559b191f1f67bb834cfc368b40.

## Apply the update

1. Close Android Studio. Extract this ZIP into a temporary directory.
2. From that directory, run PowerShell:

   powershell -ExecutionPolicy Bypass -File .\Apply-Update.ps1 -ProjectPath "D:\Others\TechPhilo\Projects\PerfectRoutine"

   The script checks the baseline hashes before changing anything. If local files
   differ, it stops and lists them instead of overwriting your work. Matching files
   are backed up inside the project's `update-backups` directory before replacement.
   Keep that backup outside Git. Only files listed in the update manifest are changed.
3. Open the original project in Android Studio and Sync Gradle.
4. Run these checks in its terminal:

   .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
   .\gradlew.bat :app:connectedDebugAndroidTest

5. With the same signing key as the installed app, use Run to update it. Do not
   uninstall or clear app storage. Database version becomes 4; do not install an
   older build declaring database version 3 over this version.
6. Top-right Settings → আমার ট্যাব lets you add, rename, color, hide, reorder, or
   remove a tab. Removing one with tasks requires a destination tab.

## Device checklist

- Updating preserves old task notes, image attachments, categories, timer records.
- Fresh install on a separate emulator starts with Home and a create-tab prompt.
- Create “University”, “ML”, and a Bangla-named tab; each can save/edit tasks.
- Rename a tab and verify its existing task remains in that tab and Home.
- Set a color; check list, task editor/detail, timer, Home, and widget colors.
- Hide the selected tab: Home becomes selected and existing reminders stay active.
- Reorder tabs; restart app and confirm order persists.
- Create at least five visible tabs; use “আরও” to open each extra tab.
- Remove a populated tab by moving tasks; task IDs, notes and time history remain.
- Last populated tab cannot be removed without another destination; it can be hidden.
- Quick list completion now marks complete rather than deleting history. Switch to
  Task list mode to access completed tasks, notes, deadlines, and timer.

## Validation and limits

SQLite schema/data-preservation smoke checks and changed-file syntax review ran.
Four Android instrumentation scenarios are added. Gradle/Android build and device
checks did not run here because the Gradle distribution download was unreachable.
This is a source update, not an APK or a build-verified release. Generate schema
4.json with Room locally. Version 1 migration is still unsupported.

This milestone covers user-managed tabs. The personal-assistant planner and the
previously reviewed focus-timer issues are separate pending work.
