package com.dupbuster.scanengine.index

import android.database.sqlite.SQLiteDatabase

/**
 * Monotonic schema upgrades (architecture §5.3). App downgrade across migrations is unsupported.
 */
object CatalogMigrator {
  const val LEGACY_VERSION: Int = 1

  fun migrate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion >= newVersion) {
      return
    }
    var version = oldVersion
    while (version < newVersion) {
      when {
        version == 1 && newVersion >= 2 -> {
          migrateV1ToV2(db)
          version = 2
        }
        else ->
            throw IllegalStateException(
                "No migration path from schema $version to $newVersion",
            )
      }
    }
  }

  /**
   * Schema v2: video metadata columns, `raw_content_fingerprint_id`, `frame_hashes_blob`,
   * `duplicate_group.match_kind`, and `full_rescan_required` (FR-IX-05).
   */
  private fun migrateV1ToV2(db: SQLiteDatabase) {
    db.execSQL("ALTER TABLE fingerprint ADD COLUMN frame_hashes_blob BLOB")
    db.execSQL("ALTER TABLE file_entry ADD COLUMN duration_ms INTEGER")
    db.execSQL("ALTER TABLE file_entry ADD COLUMN video_width INTEGER")
    db.execSQL("ALTER TABLE file_entry ADD COLUMN video_height INTEGER")
    db.execSQL(
        "ALTER TABLE file_entry ADD COLUMN raw_content_fingerprint_id INTEGER REFERENCES fingerprint(id)",
    )
    db.execSQL(
        "ALTER TABLE duplicate_group ADD COLUMN match_kind TEXT NOT NULL DEFAULT '${MatchKind.EXACT_BYTES}'",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_file_entry_duration_ms ON file_entry(duration_ms)",
    )
    db.execSQL("DELETE FROM duplicate_member")
    db.execSQL("DELETE FROM duplicate_group")
    db.execSQL(
        "INSERT OR REPLACE INTO meta (key, value) VALUES (?, ?)",
        arrayOf(CatalogSchema.META_SCHEMA_VERSION, CatalogSchema.CURRENT_VERSION.toString()),
    )
    db.execSQL(
        "INSERT OR REPLACE INTO meta (key, value) VALUES (?, ?)",
        arrayOf(CatalogSchema.META_FULL_RESCAN_REQUIRED, "1"),
    )
  }
}
