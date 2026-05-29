package com.dupbuster.scanengine.index

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * Persists scan-run checkpoints for resume (architecture §6.3 / FR-SI-03).
 * Orchestrator advances [saveCheckpoint] with monotonic `last_processed_id`; relaunch uses
 * [findResumableRun] after process kill while status remains `running` or `paused`.
 */
class CheckpointStore(private val database: CatalogDatabase) {

  fun beginRun(
      rootId: Long?,
      generation: Int,
      startedAtMs: Long = System.currentTimeMillis(),
  ): Long {
    if (hasConflictingActiveRun(rootId, generation)) {
      throw CheckpointConflictException(rootId, generation)
    }
    val values =
        ContentValues().apply {
          put("root_id", rootId)
          put("generation", generation)
          put("status", ScanRunStatus.RUNNING)
          put("last_processed_id", 0)
          put("started_at", startedAtMs)
        }
    return database.writable().insert("scan_run", null, values)
  }

  fun getRun(scanRunId: Long): ScanRunSnapshot? {
    val db = database.readable()
    db.query(
            "scan_run",
            SCAN_RUN_COLUMNS,
            "id = ?",
            arrayOf(scanRunId.toString()),
            null,
            null,
            null,
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) readSnapshot(cursor) else null
    }
  }

  /** Latest interrupted run (`running` or `paused`) for resume-on-relaunch. */
  fun findResumableRun(): ScanRunSnapshot? {
    val placeholders = ScanRunStatus.RESUMABLE.joinToString(",") { "?" }
    val args = ScanRunStatus.RESUMABLE.toTypedArray()
    val db = database.readable()
    db.query(
            "scan_run",
            SCAN_RUN_COLUMNS,
            "status IN ($placeholders)",
            args,
            null,
            null,
            "id DESC",
            "1",
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) readSnapshot(cursor) else null
    }
  }

  fun findResumableRunForRoot(rootId: Long): ScanRunSnapshot? {
    val placeholders = ScanRunStatus.RESUMABLE.joinToString(",") { "?" }
    val args = ScanRunStatus.RESUMABLE.map { it } + listOf(rootId.toString())
    val db = database.readable()
    db.query(
            "scan_run",
            SCAN_RUN_COLUMNS,
            "status IN ($placeholders) AND root_id = ?",
            args.toTypedArray(),
            null,
            null,
            "id DESC",
            "1",
        )
        .use { cursor ->
      return if (cursor.moveToFirst()) readSnapshot(cursor) else null
    }
  }

  fun hasConflictingActiveRun(rootId: Long?, generation: Int, excludeRunId: Long? = null): Boolean {
    val rootClause =
        if (rootId == null) {
          "root_id IS NULL"
        } else {
          "root_id = ?"
        }
    val activePlaceholders = ScanRunStatus.ACTIVE.joinToString(",") { "?" }
    val selection =
        "$rootClause AND generation = ? AND status IN ($activePlaceholders)" +
            if (excludeRunId != null) {
              " AND id != ?"
            } else {
              ""
            }
    val args = mutableListOf<String>()
    if (rootId != null) {
      args.add(rootId.toString())
    }
    args.add(generation.toString())
    args.addAll(ScanRunStatus.ACTIVE)
    if (excludeRunId != null) {
      args.add(excludeRunId.toString())
    }
    val db = database.readable()
    db.query(
            "scan_run",
            arrayOf("id"),
            selection,
            args.toTypedArray(),
            null,
            null,
            null,
            "1",
        )
        .use { cursor ->
      return cursor.moveToFirst()
    }
  }

  /**
   * Persists pipeline position. [lastProcessedId] must not decrease while the run is active.
   */
  fun saveCheckpoint(scanRunId: Long, lastProcessedId: Long) {
    val run =
        getRun(scanRunId)
            ?: throw IllegalArgumentException("scan_run $scanRunId not found")
    require(run.status in ScanRunStatus.RESUMABLE) {
      "Cannot checkpoint scan_run $scanRunId in status ${run.status}"
    }
    require(lastProcessedId >= run.lastProcessedId) {
      "last_processed_id must be monotonic (was ${run.lastProcessedId}, got $lastProcessedId)"
    }
    val values =
        ContentValues().apply {
          put("last_processed_id", lastProcessedId)
        }
    database
        .writable()
        .update("scan_run", values, "id = ?", arrayOf(scanRunId.toString()))
  }

  fun pauseRun(scanRunId: Long) {
    updateStatus(scanRunId, ScanRunStatus.PAUSED, requireResumable = true)
  }

  fun resumeRun(scanRunId: Long): ScanRunSnapshot {
    updateStatus(scanRunId, ScanRunStatus.RUNNING, requireResumable = true)
    return getRun(scanRunId) ?: throw IllegalStateException("scan_run $scanRunId missing after resume")
  }

  fun markCancelling(scanRunId: Long) {
    updateStatus(scanRunId, ScanRunStatus.CANCELLING, requireResumable = true)
  }

  fun markCancelled(
      scanRunId: Long,
      endedAtMs: Long = System.currentTimeMillis(),
      teardownReason: String? = null,
  ) {
    updateTerminalStatus(scanRunId, ScanRunStatus.CANCELLED, endedAtMs, teardownReason)
  }

  fun markComplete(scanRunId: Long, endedAtMs: Long = System.currentTimeMillis()) {
    updateTerminalStatus(scanRunId, ScanRunStatus.COMPLETE, endedAtMs, teardownReason = null)
  }

  fun markError(
      scanRunId: Long,
      endedAtMs: Long = System.currentTimeMillis(),
      teardownReason: String? = null,
  ) {
    updateTerminalStatus(scanRunId, ScanRunStatus.ERROR, endedAtMs, teardownReason)
  }

  /** User chose restart — terminal run; checkpoint row retained but not resumable. */
  fun abandonForRestart(scanRunId: Long, endedAtMs: Long = System.currentTimeMillis()) {
    markCancelled(scanRunId, endedAtMs, teardownReason = "USER_RESTART")
  }

  private fun updateStatus(scanRunId: Long, status: String, requireResumable: Boolean) {
    val run =
        getRun(scanRunId)
            ?: throw IllegalArgumentException("scan_run $scanRunId not found")
    if (requireResumable) {
      require(run.status in ScanRunStatus.RESUMABLE || run.status == ScanRunStatus.CANCELLING) {
        "Cannot transition scan_run $scanRunId from ${run.status} to $status"
      }
    }
    val values =
        ContentValues().apply {
          put("status", status)
        }
    database
        .writable()
        .update("scan_run", values, "id = ?", arrayOf(scanRunId.toString()))
  }

  private fun updateTerminalStatus(
      scanRunId: Long,
      status: String,
      endedAtMs: Long,
      teardownReason: String?,
  ) {
    val values =
        ContentValues().apply {
          put("status", status)
          put("ended_at", endedAtMs)
          put("teardown_reason", teardownReason)
        }
    database
        .writable()
        .update("scan_run", values, "id = ?", arrayOf(scanRunId.toString()))
  }

  private fun readSnapshot(cursor: Cursor): ScanRunSnapshot {
    val rootIdIndex = cursor.getColumnIndexOrThrow("root_id")
    return ScanRunSnapshot(
        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
        rootId = if (cursor.isNull(rootIdIndex)) null else cursor.getLong(rootIdIndex),
        generation = cursor.getInt(cursor.getColumnIndexOrThrow("generation")),
        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
        lastProcessedId = cursor.getLong(cursor.getColumnIndexOrThrow("last_processed_id")),
        startedAtMs = cursor.getLong(cursor.getColumnIndexOrThrow("started_at")),
        endedAtMs =
            cursor.getColumnIndex("ended_at").let { idx ->
              if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else null
            },
        teardownReason =
            cursor.getColumnIndex("teardown_reason").let { idx ->
              if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
            },
    )
  }

  companion object {
    private val SCAN_RUN_COLUMNS =
        arrayOf(
            "id",
            "root_id",
            "generation",
            "status",
            "last_processed_id",
            "started_at",
            "ended_at",
            "teardown_reason",
        )
  }
}
