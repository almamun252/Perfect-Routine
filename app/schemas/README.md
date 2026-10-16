# Room schema history

Keep released `2.json` and `3.json` unchanged. Version 4 introduces `user_tabs`
through `MIGRATION_3_4`, after the existing image-path `MIGRATION_2_3`.
Run the Android build to export `4.json` and commit that generated file.
The authoring environment could not download Gradle, so schema 4 is not fabricated.

A new installation starts with no task tabs. Migration 3→4 preserves legacy tabs
and any distinct task/category tab key, without editing existing task rows.
`UserTabsTest` seeds the exported v3 schema, opens it with the production Room
builder, and checks migration and preservation. Existing migration tests also
exercise 2→current. Run instrumentation tests on a device before release.

Unknown versions must never silently erase user data. Version 1's released schema
is unknown; no migration is guessed for it. Every future schema change needs a
new version, a forward migration, an exported schema, and preservation tests.
Never replace a released schema JSON or enable destructive fallback to pass a test.
