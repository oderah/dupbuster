package com.dupbuster.scanengine.index

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.dupbuster.scanengine.hash.NormalizationProfile

/**
 * Builds `duplicate_group` / `duplicate_member` rows from hashed catalog entries (M1-10).
 * Invoked after hashing (architecture grouping phase); does not cross the RN bridge.
 */
class Grouper(private val database: CatalogDatabase) {

  data class RebuildResult(
      val groupsCreated: Int,
      val totalReclaimableBytesEst: Long,
  )

  /** Clears prior groups and rebuilds from [file_entry] + [fingerprint] (FR-IX-01). */
  fun rebuildDuplicateGroups(): RebuildResult {
    val db = database.writable()
    db.beginTransaction()
    try {
      db.delete("duplicate_member", null, null)
      db.delete("duplicate_group", null, null)

      val candidates = loadGroupCandidates(db)
      val exactBytesMemberIds = mutableSetOf<Long>()
      var groupsCreated = 0
      var totalReclaimable = 0L

      val exactCandidates =
          candidates.filter { matchKindForProfile(it.normalizationProfile) == MatchKind.EXACT_BYTES }
      for (candidate in exactCandidates) {
        val groupId = insertGroup(db, candidate, MatchKind.EXACT_BYTES)
        if (groupId != null) {
          groupsCreated++
          totalReclaimable += candidate.reclaimableBytesEst
          exactBytesMemberIds.addAll(candidate.fileEntryIds)
        }
      }

      val videoCandidates =
          candidates.filter {
            matchKindForProfile(it.normalizationProfile) == MatchKind.SAME_CONTENT_VIDEO
          }
      for (candidate in videoCandidates) {
        val filteredIds = candidate.fileEntryIds.filter { it !in exactBytesMemberIds }
        if (filteredIds.size < 2) {
          continue
        }
        val filtered =
            candidate.copy(
                fileEntryIds = filteredIds,
                memberCount = filteredIds.size,
                reclaimableBytesEst = estimateReclaimableBytes(filteredIds, db),
            )
        val groupId = insertGroup(db, filtered, MatchKind.SAME_CONTENT_VIDEO)
        if (groupId != null) {
          groupsCreated++
          totalReclaimable += filtered.reclaimableBytesEst
        }
      }

      db.setTransactionSuccessful()
      return RebuildResult(groupsCreated = groupsCreated, totalReclaimableBytesEst = totalReclaimable)
    } finally {
      db.endTransaction()
    }
  }

  fun duplicateGroupCount(): Int {
    val cursor = database.readable().rawQuery("SELECT COUNT(*) FROM duplicate_group", null)
    cursor.use {
      return if (it.moveToFirst()) it.getInt(0) else 0
    }
  }

  fun memberCountForGroup(groupId: Long): Int {
    val cursor =
        database
            .readable()
            .rawQuery(
                "SELECT COUNT(*) FROM duplicate_member WHERE group_id = ?",
                arrayOf(groupId.toString()),
            )
    cursor.use {
      return if (it.moveToFirst()) it.getInt(0) else 0
    }
  }

  fun reclaimableBytesForGroup(groupId: Long): Long {
    val cursor =
        database
            .readable()
            .rawQuery(
                "SELECT reclaimable_bytes_est FROM duplicate_group WHERE id = ?",
                arrayOf(groupId.toString()),
            )
    cursor.use {
      return if (it.moveToFirst()) it.getLong(0) else 0L
    }
  }

  fun matchKindForGroup(groupId: Long): String? {
    val cursor =
        database
            .readable()
            .rawQuery(
                "SELECT match_kind FROM duplicate_group WHERE id = ?",
                arrayOf(groupId.toString()),
            )
    cursor.use {
      return if (it.moveToFirst()) it.getString(0) else null
    }
  }

  fun duplicateGroupIdsWithMatchKind(matchKind: String): List<Long> {
    val ids = mutableListOf<Long>()
    database
        .readable()
        .rawQuery(
            "SELECT id FROM duplicate_group WHERE match_kind = ? ORDER BY id ASC",
            arrayOf(matchKind),
        )
        .use { cursor ->
          while (cursor.moveToNext()) {
            ids.add(cursor.getLong(0))
          }
        }
    return ids
  }

  fun firstDuplicateGroupId(): Long? {
    database.readable().rawQuery("SELECT id FROM duplicate_group ORDER BY id ASC LIMIT 1", null).use {
      return if (it.moveToFirst()) it.getLong(0) else null
    }
  }

  companion object {
    fun matchKindForProfile(normalizationProfile: String): String =
        when (normalizationProfile) {
          NormalizationProfile.VIDEO_CONTENT_V1 -> MatchKind.SAME_CONTENT_VIDEO
          else -> MatchKind.EXACT_BYTES
        }

    /** Scan-time estimate: sum(sizes) − max(size) — one keeper assumed (FR-AC-06 preview). */
    fun estimateReclaimableBytes(sizes: List<Long>): Long {
      if (sizes.size < 2) {
        return 0L
      }
      return sizes.sum() - sizes.max()
    }
  }

  private data class FingerprintGroupCandidate(
      val fingerprintId: Long,
      val normalizationProfile: String,
      val fileEntryIds: List<Long>,
      val memberCount: Int,
      val reclaimableBytesEst: Long,
  )

  private fun loadGroupCandidates(db: SQLiteDatabase): List<FingerprintGroupCandidate> {
    val sql =
        """
        SELECT fe.fingerprint_id, f.normalization_profile, fe.id, fe.size
        FROM file_entry fe
        INNER JOIN fingerprint f ON fe.fingerprint_id = f.id
        WHERE fe.fingerprint_id IS NOT NULL
          AND fe.is_symlink = 0
          AND fe.unscannable_reason IS NULL
        ORDER BY fe.fingerprint_id ASC, fe.id ASC
        """
            .trimIndent()
    val byFingerprint = linkedMapOf<Long, MutableFingerprintAggregate>()
    db.rawQuery(sql, null).use { cursor ->
      while (cursor.moveToNext()) {
        val fingerprintId = cursor.getLong(0)
        val profile = cursor.getString(1)
        val fileEntryId = cursor.getLong(2)
        val size = cursor.getLong(3)
        val aggregate =
            byFingerprint.getOrPut(fingerprintId) {
              MutableFingerprintAggregate(fingerprintId, profile)
            }
        aggregate.fileEntryIds.add(fileEntryId)
        aggregate.sizes.add(size)
      }
    }
    return byFingerprint.values
        .filter { it.fileEntryIds.size >= 2 }
        .map { aggregate ->
          FingerprintGroupCandidate(
              fingerprintId = aggregate.fingerprintId,
              normalizationProfile = aggregate.normalizationProfile,
              fileEntryIds = aggregate.fileEntryIds.toList(),
              memberCount = aggregate.fileEntryIds.size,
              reclaimableBytesEst = estimateReclaimableBytes(aggregate.sizes),
          )
        }
  }

  private fun estimateReclaimableBytes(fileEntryIds: List<Long>, db: SQLiteDatabase): Long {
    if (fileEntryIds.isEmpty()) {
      return 0L
    }
    val placeholders = fileEntryIds.joinToString(",") { "?" }
    val args = fileEntryIds.map { it.toString() }.toTypedArray()
    val sizes = mutableListOf<Long>()
    db.rawQuery("SELECT size FROM file_entry WHERE id IN ($placeholders)", args).use { cursor ->
      while (cursor.moveToNext()) {
        sizes.add(cursor.getLong(0))
      }
    }
    return estimateReclaimableBytes(sizes)
  }

  private fun insertGroup(
      db: SQLiteDatabase,
      candidate: FingerprintGroupCandidate,
      matchKind: String,
  ): Long? {
    if (candidate.fileEntryIds.size < 2) {
      return null
    }
    val confidence =
        when (matchKind) {
          MatchKind.SAME_CONTENT_VIDEO -> MatchKind.CONFIDENCE_VIDEO_CONTENT
          else -> MatchKind.CONFIDENCE_EXACT
        }
    val groupValues =
        ContentValues().apply {
          put("fingerprint_id", candidate.fingerprintId)
          put("member_count", candidate.memberCount)
          put("reclaimable_bytes_est", candidate.reclaimableBytesEst)
          put("match_kind", matchKind)
          put("confidence_score", confidence)
        }
    val groupId = db.insert("duplicate_group", null, groupValues)
    for (fileEntryId in candidate.fileEntryIds) {
      val memberValues =
          ContentValues().apply {
            put("group_id", groupId)
            put("file_entry_id", fileEntryId)
            put("is_keeper", 0)
          }
      db.insert("duplicate_member", null, memberValues)
    }
    return groupId
  }

  private class MutableFingerprintAggregate(
      val fingerprintId: Long,
      val normalizationProfile: String,
  ) {
    val fileEntryIds = mutableListOf<Long>()
    val sizes = mutableListOf<Long>()
  }
}
