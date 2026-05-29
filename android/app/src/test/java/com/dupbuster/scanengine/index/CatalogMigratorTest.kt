package com.dupbuster.scanengine.index

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.index.MatchKind.EXACT_BYTES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CatalogMigratorTest {

  private lateinit var db: SQLiteDatabase

  @Before
  fun setUp() {
    db = SQLiteDatabase.create(null)
    CatalogSchema.applyLegacyV1Schema(db, seedMeta = true)
  }

  @Test
  fun migrateV1ToV2_addsVideoColumnsAndMatchKind() {
    seedV1CatalogData()

    CatalogMigrator.migrate(db, CatalogSchema.LEGACY_VERSION, CatalogSchema.CURRENT_VERSION)

    assertTrue(columnExists("file_entry", "duration_ms"))
    assertTrue(columnExists("file_entry", "raw_content_fingerprint_id"))
    assertTrue(columnExists("fingerprint", "frame_hashes_blob"))
    assertTrue(columnExists("duplicate_group", "match_kind"))
    assertEquals(
        CatalogSchema.CURRENT_VERSION,
        readMetaInt(CatalogSchema.META_SCHEMA_VERSION),
    )
    assertEquals(1, readMetaInt(CatalogSchema.META_FULL_RESCAN_REQUIRED))
    assertEquals(0, duplicateGroupCount())
    assertEquals(0, duplicateMemberCount())
  }

  @Test
  fun migrateV1ToV2_canInsertDuplicateGroupWithMatchKind() {
    seedV1CatalogData()
    CatalogMigrator.migrate(db, 1, 2)

    db.execSQL(
        """
        INSERT INTO duplicate_group (
          fingerprint_id, member_count, reclaimable_bytes_est, match_kind, confidence_score
        ) VALUES (1, 2, 100, '$EXACT_BYTES', 1.0)
        """
            .trimIndent(),
    )

    assertEquals(1, duplicateGroupCount())
    assertEquals(EXACT_BYTES, matchKindForGroup(1))
  }

  @Test
  fun catalogDatabase_onUpgrade_fromV1_setsFullRescanRequired() {
    db.close()
    val context: Context = ApplicationProvider.getApplicationContext()
    val name = "migration_test_${System.nanoTime()}.db"
    val v1Helper =
        object :
            android.database.sqlite.SQLiteOpenHelper(context, name, null, CatalogSchema.LEGACY_VERSION) {
          override fun onCreate(database: SQLiteDatabase) {
            CatalogSchema.applyLegacyV1Schema(database, seedMeta = true)
          }

          override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Unexpected upgrade during v1 bootstrap")
          }
        }
    v1Helper.writableDatabase.close()
    v1Helper.close()

    val v2Helper =
        object :
            android.database.sqlite.SQLiteOpenHelper(
                context,
                name,
                null,
                CatalogSchema.CURRENT_VERSION,
            ) {
          override fun onCreate(database: SQLiteDatabase) {
            error("Unexpected onCreate during v1→v2 upgrade test")
          }

          override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            CatalogMigrator.migrate(database, oldVersion, newVersion)
          }
        }
    v2Helper.writableDatabase.close()
    v2Helper.close()

    val catalogDb = CatalogDatabase(context, name)
    val meta = IndexWriter(catalogDb).readCatalogMeta()
    catalogDb.close()
    context.getDatabasePath(name).delete()

    assertEquals(CatalogSchema.CURRENT_VERSION, meta.schemaVersion)
    assertTrue(meta.fullRescanRequired)
  }

  private fun seedV1CatalogData() {
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

  private fun readMetaInt(key: String): Int? {
    db.query("meta", arrayOf("value"), "key = ?", arrayOf(key), null, null, null).use { cursor ->
      if (!cursor.moveToFirst()) {
        return null
      }
      return cursor.getString(0).toInt()
    }
  }

  private fun duplicateGroupCount(): Int {
    db.rawQuery("SELECT COUNT(*) FROM duplicate_group", null).use { cursor ->
      cursor.moveToFirst()
      return cursor.getInt(0)
    }
  }

  private fun duplicateMemberCount(): Int {
    db.rawQuery("SELECT COUNT(*) FROM duplicate_member", null).use { cursor ->
      cursor.moveToFirst()
      return cursor.getInt(0)
    }
  }

  private fun matchKindForGroup(groupId: Long): String {
    db.query("duplicate_group", arrayOf("match_kind"), "id = ?", arrayOf(groupId.toString()), null, null, null)
        .use { cursor ->
      cursor.moveToFirst()
      return cursor.getString(0)
    }
  }
}
