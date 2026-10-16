# Perfect Routine 2.3.0 — Profile and centered navigation

Apply this follow-up over the user-tabs update with the `${count}টি` interpolation
fix. The script verifies baseline hashes before writing, backs up originals,
and stops if local source differs. It does not edit .gitignore or schema JSON.

- Home stays centered with 3 or 5 slots; Profile is always at the right.
- Profile contains local name editing, task counts, tab management and reminders.
- Fresh installs seed one editable/deletable Work tab. Existing users retain
  their own tabs; removing Work does not bring it back on relaunch.
- Create/edit sheets offer 12 icons, colors, names and list modes.
- Database 4→5 adds iconKey without removing tasks or changing tab identities.

## Build and install

Run from the project root:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

After success, use Android Studio Run with the same signing key. Do not uninstall
or clear phone app data. Database becomes version 5; an older database-v4 APK
cannot open it. The source backup is not a downgrade mechanism for phone data.

## Acceptance checks

1. On the existing phone, check old tasks, notes, images and session history.
2. Open Profile → আমার ট্যাব. Create University with school icon and a color.
3. Rename and change the icon, relaunch, and confirm both persist.
4. Add/hide/reorder tabs; confirm Home stays centered and every visible tab is
   reachable. Profile must remain on the right. Check 0, 1, 2, 3 and 4+ task tabs.
5. On a separate clean emulator, verify Work · Home · Profile. Delete the empty
   Work tab, relaunch, and confirm it does not return. Do not clear the real phone.
6. Check tab editor scrolling with keyboard open and larger system font.
7. Confirm a populated tab requires a destination before deletion.

## Validation limits

Source syntax parsing, git diff checks and SQLite SQL smoke checks passed here.
Android compilation, Compose rendering and the newly added JUnit/device tests
were not run here. The previous update's successful device run does not validate
this new update. This archive contains source, not a compiled APK.
