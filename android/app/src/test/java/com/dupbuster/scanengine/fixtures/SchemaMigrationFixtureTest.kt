package com.dupbuster.scanengine.fixtures

import android.database.sqlite.SQLiteDatabase
import com.dupbuster.scanengine.index.CatalogMigrator
import com.dupbuster.scanengine.index.CatalogSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Descriptor-backed v1→v2 migration expectations (index-schema-migration-01 / M4-15). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SchemaMigrationFixtureTest {

  private lateinit var db: SQLiteDatabase

  @Before
  fun setUp() {
    db = SQLiteDatabase.create(null)
    CatalogSchema.applyLegacyV1Schema(db, seedMeta = true)
    db.execSQL(
        "INSERT INTO scan_root (uri_or_grant, mode, created_at) VALUES ('content://test', 'user_selected', 1)",
    )
    db.execSQL(
        """
        INSERT INTO fingerprint (hash_algo, hash_value, normalization_profile, computed_at)
        VALUES ('SHA256', 'abc', 'RAW_BYTES', 1)
        """
            .trimIndent(),
    )
    db.execSQL(
        """
        INSERT INTO file_entry (
          root_id, uri_or_path, display_name, size, mtime_ns, fingerprint_id, last_seen_generation, is_symlink
        ) VALUES (1, 'content://a', 'a.bin', 100, 0, 1, 1, 0)
        """
            .trimIndent(),
    )
    db.execSQL(
        """
        INSERT INTO duplicate_group (fingerprint_id, member_count, reclaimable_bytes_est, confidence_score)
        VALUES (1, 2, 50, 1.0)
        """
            .trimIndent(),
    )
    db.execSQL(
        "INSERT INTO duplicate_member (group_id, file_entry_id, is_keeper) VALUES (1, 1, 0)",
    )
  }

  @Test
  fun indexSchemaMigration01_matchesFixtureExpect() {
    val fixture = FixtureLoader.load("index-schema-migration-01.json")
    assertEquals("index-schema-migration-01", fixture.getString("id"))
    assertEquals("both", fixture.getString("platform"))

    val input = fixture.getJSONObject("input")
    assertEquals(1, input.getInt("fromSchemaVersion"))
    assertEquals(2, input.getInt("toSchemaVersion"))

    val expect = fixture.getJSONObject("expect")
    val fromVersion = input.getInt("fromSchemaVersion")
    val toVersion = input.getInt("toSchemaVersion")

    CatalogMigrator.migrate(db, fromVersion, toVersion)

    assertEquals(expect.getInt("schemaVersion"), readMetaInt(CatalogSchema.META_SCHEMA_VERSION))
    assertEquals(
        if (expect.getBoolean("fullRescanRequired")) 1 else 0,
        readMetaInt(CatalogSchema.META_FULL_RESCAN_REQUIRED),
    )

    for (i in 0 until expect.getJSONArray("columnsAdded").length()) {
      val qualified = expect.getJSONArray("columnsAdded").getString(i)
      val parts = qualified.split(".", limit = 2)
      assertTrue(
          "missing column $qualified",
          columnExists(parts[0], parts[1]),
      )
    }

    if (expect.getBoolean("duplicateGroupsCleared")) {
      assertEquals(0, countRows("duplicate_group"))
      assertEquals(0, countRows("duplicate_member"))
    }
  }

  private fun readMetaInt(key: String): Int? {
    db.query("meta", arrayOf("value"), "key = ?", arrayOf(key), null, null, null).use { cursor ->
      if (!cursor.moveToFirst()) {
        return null
      }
      return cursor.getString(0).toInt()
    }
  }

  private fun columnExists(table: String, column: String): Boolean {
    db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
      while (cursor.moveToNext()) {
        if (column == cursor.getString(cursor.getColumnIndexOrThrow("name"))) {
          return true
        }
      }
    }
    return false
  }

  private fun countRows(table: String): Int {
    db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
      cursor.moveToFirst()
      return cursor.getInt(0)
    }
  }
}
