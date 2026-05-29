package com.dupbuster.scanengine.index

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.dupbuster.scanengine.hash.HashResult
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.stat.StagedFile

/**
 * SQLite CRUD for the merged catalog (architecture §5 / FR-IX-*).
 * Persists discovery → hash outcomes; [Grouper] writes duplicate_group/member after hashing.
 */
class IndexWriter(private val database: CatalogDatabase) {

  fun readCatalogMeta(): CatalogMeta {
    val db = database.readable()
    val schemaVersion =
        readMetaInt(db, CatalogSchema.META_SCHEMA_VERSION) ?: CatalogSchema.CURRENT_VERSION
    val fullRescan =
        readMetaInt(db, CatalogSchema.META_FULL_RESCAN_REQUIRED)?.let { it != 0 } ?: false
    return CatalogMeta(schemaVersion = schemaVersion, fullRescanRequired = fullRescan)
  }

  fun insertScanRoot(
      uriOrGrant: String,
      mode: ScanRootMode,
      platformReason: String? = null,
      createdAtMs: Long = System.currentTimeMillis(),
  ): Long {
    val values =
        ContentValues().apply {
          put("uri_or_grant", uriOrGrant)
          put("mode", mode.name.lowercase())
          put("platform_reason", platformReason)
          put("created_at", createdAtMs)
        }
    return database.writable().insert("scan_root", null, values)
  }

  fun beginScanRun(
      generation: Int,
      rootId: Long? = null,
      startedAtMs: Long = System.currentTimeMillis(),
  ): Long {
    val values =
        ContentValues().apply {
          put("root_id", rootId)
          put("generation", generation)
          put("status", "running")
          put("last_processed_id", 0)
          put("started_at", startedAtMs)
        }
    return database.writable().insert("scan_run", null, values)
  }

  fun updateScanRunCheckpoint(scanRunId: Long, lastProcessedId: Long) {
    val values =
        ContentValues().apply {
          put("last_processed_id", lastProcessedId)
        }
    database
        .writable()
        .update("scan_run", values, "id = ?", arrayOf(scanRunId.toString()))
  }

  fun completeScanRun(scanRunId: Long, endedAtMs: Long = System.currentTimeMillis()) {
    val values =
        ContentValues().apply {
          put("status", "complete")
          put("ended_at", endedAtMs)
        }
    database
        .writable()
        .update("scan_run", values, "id = ?", arrayOf(scanRunId.toString()))
  }

  /** Persists one hash-pipeline outcome and returns `file_entry.id`. */
  fun persistHashResult(result: HashResult, staged: StagedFile, generation: Int): Long {
    return when (result) {
      is HashResult.Success -> upsertHashed(result.hashed, generation)
      is HashResult.SizeBucketSkipped -> upsertSizeBucketSkipped(result.staged, generation)
      is HashResult.SymlinkNode -> upsertSymlink(result.staged, generation)
      is HashResult.Unscannable -> upsertUnscannable(result.reason, staged, generation)
    }
  }

  fun upsertHashed(hashed: HashedFile, generation: Int): Long {
    val staged = hashed.staged
    val fingerprintId =
        getOrCreateFingerprint(
            hashValue = hashed.hashValue,
            normalizationProfile = hashed.normalizationProfile,
        )
    return upsertFileEntry(
        staged = staged,
        generation = generation,
        fingerprintId = fingerprintId,
        unscannableReason = null,
        isSymlink = false,
    )
  }

  fun upsertSizeBucketSkipped(staged: StagedFile, generation: Int): Long =
      upsertFileEntry(
          staged = staged,
          generation = generation,
          fingerprintId = null,
          unscannableReason = null,
          isSymlink = false,
      )

  fun upsertSymlink(staged: StagedFile, generation: Int): Long =
      upsertFileEntry(
          staged = staged,
          generation = generation,
          fingerprintId = null,
          unscannableReason = null,
          isSymlink = true,
      )

  fun upsertUnscannable(reason: String, staged: StagedFile, generation: Int): Long =
      upsertFileEntry(
          staged = staged,
          generation = generation,
          fingerprintId = null,
          unscannableReason = reason,
          isSymlink = staged.isSymlink,
      )

  /**
   * Counts indexed rows with [sizeBytes] for size-bucket elimination (FR-FP-02).
   * Excludes video rows (video always hashes).
   */
  fun countIndexedFilesWithSize(sizeBytes: Long): Int {
    val cursor =
        database
            .readable()
            .rawQuery(
                "SELECT COUNT(*) FROM file_entry WHERE size = ?",
                arrayOf(sizeBytes.toString()),
            )
    cursor.use {
      return if (it.moveToFirst()) it.getInt(0) else 0
    }
  }

  fun findFingerprintId(hashValue: String, normalizationProfile: String): Long? {
    val db = database.readable()
    db.query(
            "fingerprint",
            arrayOf("id"),
            "hash_algo = ? AND hash_value = ? AND normalization_profile = ?",
            arrayOf(CatalogSchema.HASH_ALGO_SHA256, hashValue, normalizationProfile),
            null,
            null,
            null,
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) cursor.getLong(0) else null
    }
  }

  /** Deletes entries under [rootId] not seen in [generation] (FR-IX-03 purge step). */
  fun purgeEntriesNotSeenInGeneration(rootId: Long, generation: Int): Int {
    val db = database.writable()
    db.delete(
        "file_path",
        """
        file_entry_id IN (
          SELECT id FROM file_entry
          WHERE root_id = ? AND last_seen_generation < ?
        )
        """
            .trimIndent(),
        arrayOf(rootId.toString(), generation.toString()),
    )
    return db.delete(
        "file_entry",
        "root_id = ? AND last_seen_generation < ?",
        arrayOf(rootId.toString(), generation.toString()),
    )
  }

  fun fileEntryCount(): Int {
    val cursor =
        database.readable().rawQuery("SELECT COUNT(*) FROM file_entry", null)
    cursor.use {
      return if (it.moveToFirst()) it.getInt(0) else 0
    }
  }

  private fun upsertFileEntry(
      staged: StagedFile,
      generation: Int,
      fingerprintId: Long?,
      unscannableReason: String?,
      isSymlink: Boolean,
  ): Long {
    val db = database.writable()
    db.beginTransaction()
    try {
      val inode = staged.inode
      val deviceId = staged.deviceId
      if (inode != null && deviceId != null) {
        findFileEntryIdByInodeDevice(inode, deviceId)?.let { existingId ->
          val uri = uriOrPath(staged)
          val primaryUri = primaryUriForFileEntry(existingId)
          if (primaryUri != null && primaryUri != uri) {
            insertPathAlias(existingId, uri)
          }
          updateFileEntryRow(
              fileEntryId = existingId,
              staged = staged,
              generation = generation,
              fingerprintId = fingerprintId,
              unscannableReason = unscannableReason,
              isSymlink = isSymlink,
          )
          db.setTransactionSuccessful()
          return existingId
        }
      }

      val rootId = staged.discovered.scanRootId
      val uri = uriOrPath(staged)
      val existingId = findFileEntryIdByUri(rootId, uri)
      val fileEntryId =
          if (existingId != null) {
            updateFileEntryRow(
                fileEntryId = existingId,
                staged = staged,
                generation = generation,
                fingerprintId = fingerprintId,
                unscannableReason = unscannableReason,
                isSymlink = isSymlink,
            )
            existingId
          } else {
            insertFileEntryRow(
                staged = staged,
                generation = generation,
                fingerprintId = fingerprintId,
                unscannableReason = unscannableReason,
                isSymlink = isSymlink,
            )
          }
      db.setTransactionSuccessful()
      return fileEntryId
    } finally {
      db.endTransaction()
    }
  }

  private fun getOrCreateFingerprint(
      hashValue: String,
      normalizationProfile: String,
      computedAtMs: Long = System.currentTimeMillis(),
  ): Long {
    findFingerprintId(hashValue, normalizationProfile)?.let {
      return it
    }
    val values =
        ContentValues().apply {
          put("hash_algo", CatalogSchema.HASH_ALGO_SHA256)
          put("hash_value", hashValue)
          put("normalization_profile", normalizationProfile)
          put("computed_at", computedAtMs)
        }
    return database.writable().insert("fingerprint", null, values)
  }

  private fun insertFileEntryRow(
      staged: StagedFile,
      generation: Int,
      fingerprintId: Long?,
      unscannableReason: String?,
      isSymlink: Boolean,
  ): Long {
    val discovered = staged.discovered
    val values =
        ContentValues().apply {
          put("root_id", discovered.scanRootId)
          put("uri_or_path", uriOrPath(staged))
          put("display_name", discovered.displayName)
          put("size", staged.sizeBytes)
          put("mtime_ns", staged.mtimeNs)
          put("fingerprint_id", fingerprintId)
          put("last_seen_generation", generation)
          put("is_symlink", if (isSymlink) 1 else 0)
          put("unscannable_reason", unscannableReason)
          put("inode", staged.inode)
          put("device_id", staged.deviceId)
        }
    return database.writable().insert("file_entry", null, values)
  }

  private fun updateFileEntryRow(
      fileEntryId: Long,
      staged: StagedFile,
      generation: Int,
      fingerprintId: Long?,
      unscannableReason: String?,
      isSymlink: Boolean,
  ): Long {
    val values =
        ContentValues().apply {
          put("display_name", staged.discovered.displayName)
          put("size", staged.sizeBytes)
          put("mtime_ns", staged.mtimeNs)
          if (fingerprintId != null) {
            put("fingerprint_id", fingerprintId)
          }
          put("last_seen_generation", generation)
          put("is_symlink", if (isSymlink) 1 else 0)
          put("unscannable_reason", unscannableReason)
          put("inode", staged.inode)
          put("device_id", staged.deviceId)
        }
    database
        .writable()
        .update("file_entry", values, "id = ?", arrayOf(fileEntryId.toString()))
    return fileEntryId
  }

  private fun insertPathAlias(fileEntryId: Long, aliasPath: String) {
    val values =
        ContentValues().apply {
          put("file_entry_id", fileEntryId)
          put("alias_path", aliasPath)
        }
    database.writable().insertWithOnConflict("file_path", null, values, SQLiteDatabase.CONFLICT_IGNORE)
  }

  private fun findFileEntryIdByInodeDevice(inode: Long, deviceId: Long): Long? {
    val db = database.readable()
    db.query(
            "file_entry",
            arrayOf("id"),
            "inode = ? AND device_id = ?",
            arrayOf(inode.toString(), deviceId.toString()),
            null,
            null,
            "id ASC",
            "1",
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) cursor.getLong(0) else null
    }
  }

  private fun findFileEntryIdByUri(rootId: Long, uriOrPath: String): Long? {
    val db = database.readable()
    db.query(
            "file_entry",
            arrayOf("id"),
            "root_id = ? AND uri_or_path = ?",
            arrayOf(rootId.toString(), uriOrPath),
            null,
            null,
            null,
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) cursor.getLong(0) else null
    }
  }

  private fun primaryUriForFileEntry(fileEntryId: Long): String? {
    val db = database.readable()
    db.query(
            "file_entry",
            arrayOf("uri_or_path"),
            "id = ?",
            arrayOf(fileEntryId.toString()),
            null,
            null,
            null,
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) cursor.getString(0) else null
    }
  }

  private fun uriOrPath(staged: StagedFile): String = staged.discovered.contentUri.toString()

  private fun readMetaInt(db: SQLiteDatabase, key: String): Int? {
    db.query("meta", arrayOf("value"), "key = ?", arrayOf(key), null, null, null).use { cursor ->
      if (!cursor.moveToFirst()) {
        return null
      }
      return cursor.getString(0).toIntOrNull()
    }
  }
}
