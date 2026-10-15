# Room schema history

Build the app before making any database schema change. Kapt exports the
current version's JSON here under `com.almamun252.perfectroutine.data.AppDatabase/`.
Keep generated `2.json` if it was exported by the previous build; build this
update to export `3.json`, and commit both available versions to Git. Version 3
adds imagePaths with a TEXT NOT NULL default of '[]' via MIGRATION_2_3.
No generated schema is supplied in this
patch because its Gradle build could not run in the authoring environment.

Keep every released version's JSON. Never edit or overwrite a released
schema JSON to make a migration pass. Version 1's actual schema is unknown;
no migration from version 1 is guessed or registered.

For each future structural change:

1. Keep the previous JSON and increase `@Database.version`.
2. Implement/register a data-preserving migration in `DatabaseMigrations.ALL`,
   or use a Room `AutoMigration` when the change is unambiguous and supported.
3. Build to export the new schema.
4. Use `MigrationTestHelper` from `room-testing` to validate the real old-to-new
   schema migration and assert that tasks, categories, and time sessions survive.
5. Test updating an installed APK with existing data using the same signing key.

Do not add destructive fallback or delete the database to handle errors.
