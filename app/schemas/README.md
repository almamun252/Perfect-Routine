# Room schema history

Preserve every released schema, including locally generated 4.json and 5.json.
Version 6 adds nullable INTEGER columns startDate and sortOrder to task_table.
MIGRATION_5_6 initializes both from the stored taskDate, leaving all prior columns
and task IDs unchanged. Future task creation keeps creation/start/order separate.
If an older version already changed taskDate while reordering, the original date
cannot be reconstructed; migration preserves the value actually stored.

Build to generate 6.json, then commit it with source. This source update does not
fabricate or overwrite schema JSON. TaskScheduleMigrationTest covers v5 upgrade,
order-only updates against stale task snapshots and invalid schedule rejection.
Earlier tests exercise v2/v3/v4→current migrations and unknown-version preservation.
Fresh installs still seed Work once. No destructive fallback is enabled.
