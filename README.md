# Perfect Routine

Perfect Routine is an Android productivity app for organizing tasks, managing deadlines, and tracking focused work sessions. It is built with Kotlin and Jetpack Compose, with Room used for local data persistence.

## Features

### User-controlled tabs
- Home stays in the physical center of the bottom bar; Profile contains tab management and reminder settings.
- A fresh install starts with Work · Home · Profile. Work can be renamed, hidden, or deleted and is seeded only once.
- Existing installations retain Learning, Research, Work, Temp, and every historic task/category tab key through a non-destructive 3→4 migration.
- Rename tabs without changing task membership; choose from 12 icons and colors, hide tabs, and reorder them.
- Use the common Task list or opt into Quick list mode for any tab.
- The bar uses 3 or 5 slots to keep Home centered, with a tab picker when needed.
- Hiding only affects navigation: tasks remain in Home, widgets, and reminders.
- Removing a populated tab requires moving its tasks into another tab, preserving task IDs, attached images, deadlines, and time sessions.

### Task management
- Organize tasks under Learning, Research, and Work.
- Capture quick tasks in the Temp list.
- Reorder Temp tasks using drag and drop.
- Add a task title, description, category, priority, and optional deadline.
- View task details, completion status, deadline countdown, and recorded time spent.
- Filter tasks by date range and review task summaries, including overdue tasks.
- Attach images to task notes.
- Create and manage categories.

### Focus timer and time tracking
- Choose between Pomodoro and Stopwatch modes.
- Configure the Pomodoro duration.
- Associate a focus session with a task.
- Save focus-session records and calculate the total recorded time for a task.
- Display an ongoing notification while the focus service is active.
- Use optional strict-focus reminders at 15-minute intervals while the reminder service is active.
- Select a custom sound for strict-focus reminders.

### Deadlines and notifications
- Schedule task deadline reminders.
- Use Android exact alarms when permitted, with WorkManager as a fallback.
- Update reminder schedules when tasks change.
- Handle relevant system events such as device boot, time changes, timezone changes, and package replacement.
- Configure notification-related settings.

### Home-screen widget
- View task information from an Android home-screen widget.
- Complete a task or refresh widget content through its actions.
- Display information from the app's local task database.

### Local data storage
- Store tasks, categories, and focus-session records using Room.
- Export Room database schemas.
- Migrate 2→3 to add task image paths, then 3→4 to add user-managed tabs and 4→5 to persist selected icons, and 5→6 to separate start dates and quick-list order.
- Store attached images in app-private storage and save their relative paths in the database.
- Avoid destructive fallback migrations that could silently erase saved data when the database schema is unknown.

## Technology stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Architecture | MVVM-style ViewModels and repository-based data access |
| Local database | Room |
| Reactive state and asynchronous work | Kotlin Coroutines, Flow, StateFlow |
| Deadline scheduling | AlarmManager and WorkManager |
| Focus notification | Android Foreground Service |
| Home-screen widget | AndroidX Glance AppWidget |
| Testing | JUnit and Android instrumentation tests |

## Project structure

```text
app/src/main/
├── AndroidManifest.xml
└── java/com/almamun252/perfectroutine/
    ├── data/        # Database, DAOs, migrations, repository, image storage
    ├── model/       # Task, Category, and TimeSession models
    ├── service/     # Focus timer foreground service
    ├── utils/       # Date filtering, constants, and time formatting
    ├── view/        # Screens and UI components
    ├── viewmodel/   # Task and timer state and operations
    ├── widget/      # Home-screen task widget
    └── worker/      # Deadline scheduling, receivers, workers, notifications
```

## Requirements

The Android module is configured with the following settings:

- **Minimum SDK:** Android 8.0 (API 26)
- **Compile SDK:** 35
- **Target SDK:** 35
- **Version name:** 2.4.0
- **Version code:** 4

Use a compatible version of Android Studio and the JDK required by the project's Gradle configuration. The Gradle wrapper and version catalog define the build-tool and dependency versions.

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/almamun252/Perfect-Routine.git
cd Perfect-Routine
```

### 2. Open the project

Open the project directory in Android Studio and allow Gradle to sync. Make sure the required Android SDK is installed.

### 3. Build the debug APK

**Windows**

```bat
gradlew.bat assembleDebug
```

**macOS / Linux**

```bash
./gradlew assembleDebug
```

The debug APK is normally generated under:

```text
app/build/outputs/apk/debug/
```

### 4. Install on a connected device

**Windows**

```bat
gradlew.bat installDebug
```

**macOS / Linux**

```bash
./gradlew installDebug
```

## Running tests

Run local unit tests:

```bash
./gradlew test
```

Run Android instrumentation tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Notifications and background behavior

The app declares permissions related to notifications, exact alarms, boot handling, and foreground-service operation. Depending on the Android version and device settings, the user may need to grant notification permission or allow exact alarms in system settings.

When exact alarms are unavailable, deadline reminders may use WorkManager. WorkManager is designed for deferrable background work and does not guarantee execution at an exact second. Battery optimization, manufacturer-specific restrictions, notification-channel settings, and Android version can affect background work and notification delivery.

## Data and privacy

Tasks, categories, focus-session records, and attached images are stored locally by the app. The inspected Android manifest does not declare the `INTERNET` permission. Device backup behavior depends on the app's backup configuration and Android settings.

## Developer

**Abdullah Al Mamun**  
Computer Science & Engineering  
Daffodil International University

## License

No license file is included in this repository. Unless a license is added, users should not assume that redistribution or modification is permitted.

## Version 2.4.0 — task schedules and reliability review

- Choose a start date/time and optional deadline in both task and quick-list forms.
  Omitted start uses the save time; edits retain the original start unless changed.
- Creation timestamp, start timestamp and quick-list order are separate. Reordering
  cannot change dates, notes, completion status or a newer task edit.
- Date-picker values are converted from UTC calendar dates to local wall time.
  A newly selected deadline before start is rejected. Unchanged historic overdue
  schedules remain editable.
- Profile uses a centered avatar, simple name editing, compact counts and settings.
- Quick-list titles open task details/edit. Save errors are surfaced.
- Focus timer uses monotonic elapsed time, preserves accumulated duration across
  mode switches, and awaits session storage before reporting success.

The user's 2.3.0 build/unit tests and 11 Android device tests passed on V2126,
Android 14. Version 2.4.0 is a NEW source update requiring its own validation.
The authoring environment cannot compile/render Android here. See
`docs/APP-AUDIT-2.4.0.md` for reviewed behavior, limitations and a device checklist.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Retain exported schemas 2–5 and generate `6.json` during the build. Never uninstall
or clear phone data to resolve a migration failure. Keep the same signing key.
The personal-assistant scheduler and process-death recovery for active focus
sessions remain future work; this review is not a guarantee of a bug-free app.
