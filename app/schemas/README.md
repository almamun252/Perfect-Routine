# Room schema history

Keep all released schemas unchanged, including locally generated `4.json`.
Version 4 adds user_tabs; version 5 adds iconKey with the SQL default 'folder'.
The 4→5 migration assigns legacy icons while preserving tab IDs, names, order,
visibility, colors and tasks. A fresh database seeds one editable Work tab in
onCreate; upgrades and reopening a database never recreate a deleted Work tab.

Build to generate `5.json` and retain it with schema history. The authoring
machine cannot run the Android build, so no schema JSON is fabricated here.
UserTabsTest covers v3 and v4 upgrades and fresh-install behavior using the
production Room builder. Existing image migration tests exercise 2→current.
Run device tests before release. Version 1's schema remains unknown; never
use destructive fallback or replace a released schema to pass validation.
