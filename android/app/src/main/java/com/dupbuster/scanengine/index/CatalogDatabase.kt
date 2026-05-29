package com.dupbuster.scanengine.index

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Opens the merged DupBuster catalog database (FR-IX-01). */
class CatalogDatabase(
    context: Context,
    name: String = CatalogSchema.DATABASE_NAME,
) : SQLiteOpenHelper(context.applicationContext, name, null, CatalogSchema.CURRENT_VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    applySchema(db, isFreshInstall = true)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    CatalogMigrator.migrate(db, oldVersion, newVersion)
  }

  fun writable(): SQLiteDatabase = writableDatabase

  fun readable(): SQLiteDatabase = readableDatabase

  companion object {
    @Volatile private var instance: CatalogDatabase? = null

    fun getInstance(context: Context): CatalogDatabase =
        instance
            ?: synchronized(this) {
              instance ?: CatalogDatabase(context).also { instance = it }
            }

    /** In-memory catalog for unit tests. */
    fun inMemory(context: Context): CatalogDatabase =
        CatalogDatabase(context, name = ":memory:").apply { setWriteAheadLoggingEnabled(false) }

    internal fun applySchema(db: SQLiteDatabase, isFreshInstall: Boolean) {
      db.execSQL("PRAGMA foreign_keys=ON")
      for (statement in CatalogSchema.CREATE_TABLES) {
        db.execSQL(statement)
      }
      for (statement in CatalogSchema.CREATE_INDEXES) {
        db.execSQL(statement)
      }
      if (isFreshInstall) {
        val now = System.currentTimeMillis().toString()
        db.execSQL(
            "INSERT OR REPLACE INTO meta (key, value) VALUES (?, ?)",
            arrayOf(CatalogSchema.META_SCHEMA_VERSION, CatalogSchema.CURRENT_VERSION.toString()),
        )
        db.execSQL(
            "INSERT OR REPLACE INTO meta (key, value) VALUES (?, ?)",
            arrayOf(CatalogSchema.META_FULL_RESCAN_REQUIRED, "0"),
        )
      }
    }
  }
}
