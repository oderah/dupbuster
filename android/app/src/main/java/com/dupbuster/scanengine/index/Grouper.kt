package com.dupbuster.scanengine.index

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.dupbuster.scanengine.hash.ImageContentMatcher
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.hash.VideoContentMatcher
import com.dupbuster.scanengine.hash.VideoFingerprint
import com.dupbuster.scanengine.hash.VideoFingerprintCodec

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

      val exactBytesMemberIds = mutableSetOf<Long>()
      var groupsCreated = 0
      var totalReclaimable = 0L

      val exactCandidates = loadExactBytesCandidates(db)
      for (candidate in exactCandidates) {
        val groupId = insertGroup(db, candidate, MatchKind.EXACT_BYTES)
        if (groupId != null) {
          groupsCreated++
          totalReclaimable += candidate.reclaimableBytesEst
          exactBytesMemberIds.addAll(candidate.fileEntryIds)
        }
      }

      val videoClusters = clusterVideoContentEntries(loadVideoContentEntries(db))
      for (cluster in videoClusters) {
        if (cluster.candidate.fileEntryIds.size < 2) {
          continue
        }
        val groupId = insertGroup(db, cluster.candidate, cluster.matchKind)
        if (groupId != null) {
          groupsCreated++
          totalReclaimable += cluster.candidate.reclaimableBytesEst
          if (cluster.matchKind == MatchKind.EXACT_BYTES) {
            exactBytesMemberIds.addAll(cluster.candidate.fileEntryIds)
          }
        }
      }

      val imageClusters = clusterImageContentEntries(loadImageContentEntries(db))
      for (cluster in imageClusters) {
        if (cluster.candidate.fileEntryIds.size < 2) {
          continue
        }
        val groupId = insertGroup(db, cluster.candidate, cluster.matchKind)
        if (groupId != null) {
          groupsCreated++
          totalReclaimable += cluster.candidate.reclaimableBytesEst
          if (cluster.matchKind == MatchKind.EXACT_BYTES) {
            exactBytesMemberIds.addAll(cluster.candidate.fileEntryIds)
          }
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
          NormalizationProfile.IMAGE_CONTENT_V1 -> MatchKind.SAME_CONTENT_IMAGE
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

  private fun loadExactBytesCandidates(db: SQLiteDatabase): List<FingerprintGroupCandidate> {
    val sql =
        """
        SELECT COALESCE(fe.raw_content_fingerprint_id, fe.fingerprint_id) AS group_fp_id,
               f.normalization_profile, fe.id, fe.size
        FROM file_entry fe
        INNER JOIN fingerprint f ON f.id = COALESCE(fe.raw_content_fingerprint_id, fe.fingerprint_id)
        WHERE fe.fingerprint_id IS NOT NULL
          AND fe.is_symlink = 0
          AND fe.unscannable_reason IS NULL
          AND fe.raw_content_fingerprint_id IS NULL
          AND f.normalization_profile NOT IN (
            '${NormalizationProfile.VIDEO_CONTENT_V1}',
            '${NormalizationProfile.IMAGE_CONTENT_V1}'
          )
        ORDER BY group_fp_id ASC, fe.id ASC
        """
            .trimIndent()
    return aggregateCandidates(db, sql)
  }

  private data class VideoContentEntry(
      val fileEntryId: Long,
      val fingerprintId: Long,
      val frameHashes: LongArray,
      val durationMs: Long,
      val videoWidth: Int,
      val videoHeight: Int,
      val sizeBytes: Long,
      val rawContentFingerprintId: Long?,
  ) {
    fun toVideoFingerprint(): VideoFingerprint =
        VideoFingerprint(
            frameHashes = frameHashes,
            durationMs = durationMs,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
        )
  }

  private data class VideoContentCluster(
      val candidate: FingerprintGroupCandidate,
      val matchKind: String,
  )

  private fun loadVideoContentEntries(db: SQLiteDatabase): List<VideoContentEntry> {
    val sql =
        """
        SELECT fe.id, fe.fingerprint_id, f.frame_hashes_blob, fe.size, fe.raw_content_fingerprint_id,
               fe.duration_ms, fe.video_width, fe.video_height
        FROM file_entry fe
        INNER JOIN fingerprint f ON fe.fingerprint_id = f.id
        WHERE fe.fingerprint_id IS NOT NULL
          AND fe.is_symlink = 0
          AND fe.unscannable_reason IS NULL
          AND f.normalization_profile = '${NormalizationProfile.VIDEO_CONTENT_V1}'
          AND f.frame_hashes_blob IS NOT NULL
        ORDER BY fe.id ASC
        """
            .trimIndent()
    val entries = mutableListOf<VideoContentEntry>()
    db.rawQuery(sql, null).use { cursor ->
      while (cursor.moveToNext()) {
        val blob = cursor.getBlob(2) ?: continue
        val hashes = VideoFingerprintCodec.decodeFrameHashesBlob(blob)
        if (hashes.isEmpty()) {
          continue
        }
        val durationMs = if (cursor.isNull(5)) 0L else cursor.getLong(5)
        entries.add(
            VideoContentEntry(
                fileEntryId = cursor.getLong(0),
                fingerprintId = cursor.getLong(1),
                frameHashes = hashes,
                durationMs = durationMs,
                videoWidth = if (cursor.isNull(6)) 0 else cursor.getInt(6),
                videoHeight = if (cursor.isNull(7)) 0 else cursor.getInt(7),
                sizeBytes = cursor.getLong(3),
                rawContentFingerprintId =
                    if (cursor.isNull(4)) {
                      null
                    } else {
                      cursor.getLong(4)
                    },
            ),
        )
      }
    }
    return entries
  }

  private fun clusterVideoContentEntries(entries: List<VideoContentEntry>): List<VideoContentCluster> {
    if (entries.size < 2) {
      return emptyList()
    }

    val parent = IntArray(entries.size) { it }
    fun find(index: Int): Int {
      var root = index
      while (parent[root] != root) {
        root = parent[root]
      }
      var node = index
      while (parent[node] != node) {
        val next = parent[node]
        parent[node] = root
        node = next
      }
      return root
    }
    fun union(left: Int, right: Int) {
      val rootLeft = find(left)
      val rootRight = find(right)
      if (rootLeft != rootRight) {
        parent[rootRight] = rootLeft
      }
    }

    for (i in entries.indices) {
      for (j in i + 1 until entries.size) {
        if (
            VideoContentMatcher.contentMatches(
                entries[i].toVideoFingerprint(),
                entries[j].toVideoFingerprint(),
            )
        ) {
          union(i, j)
        }
      }
    }

    val clusters = linkedMapOf<Int, MutableList<VideoContentEntry>>()
    for (index in entries.indices) {
      val root = find(index)
      clusters.getOrPut(root) { mutableListOf() }.add(entries[index])
    }

    return clusters.values
        .filter { it.size >= 2 }
        .map { cluster ->
          val fileEntryIds = cluster.map { it.fileEntryId }
          val sizes = cluster.map { it.sizeBytes }
          VideoContentCluster(
              candidate =
                  FingerprintGroupCandidate(
                      fingerprintId = cluster.first().fingerprintId,
                      normalizationProfile = NormalizationProfile.VIDEO_CONTENT_V1,
                      fileEntryIds = fileEntryIds,
                      memberCount = fileEntryIds.size,
                      reclaimableBytesEst = estimateReclaimableBytes(sizes),
                  ),
              matchKind = matchKindForVideoCluster(cluster),
          )
        }
  }

  /** AC-equiv-video-xres-04: byte-identical only → `EXACT_BYTES`; cross-resolution → `SAME_CONTENT_VIDEO`. */
  private fun matchKindForVideoCluster(cluster: List<VideoContentEntry>): String {
    val rawIds = cluster.map { it.rawContentFingerprintId }
    return if (rawIds.all { it != null } && rawIds.toSet().size == 1) {
      MatchKind.EXACT_BYTES
    } else {
      MatchKind.SAME_CONTENT_VIDEO
    }
  }

  private data class ImageContentEntry(
      val fileEntryId: Long,
      val fingerprintId: Long,
      val dHash: Long,
      val sizeBytes: Long,
      val rawContentFingerprintId: Long?,
  )

  private data class ImageContentCluster(
      val candidate: FingerprintGroupCandidate,
      val matchKind: String,
  )

  private fun loadImageContentEntries(db: SQLiteDatabase): List<ImageContentEntry> {
    val sql =
        """
        SELECT fe.id, fe.fingerprint_id, f.frame_hashes_blob, fe.size, fe.raw_content_fingerprint_id
        FROM file_entry fe
        INNER JOIN fingerprint f ON fe.fingerprint_id = f.id
        WHERE fe.fingerprint_id IS NOT NULL
          AND fe.is_symlink = 0
          AND fe.unscannable_reason IS NULL
          AND f.normalization_profile = '${NormalizationProfile.IMAGE_CONTENT_V1}'
          AND f.frame_hashes_blob IS NOT NULL
        ORDER BY fe.id ASC
        """
            .trimIndent()
    val entries = mutableListOf<ImageContentEntry>()
    db.rawQuery(sql, null).use { cursor ->
      while (cursor.moveToNext()) {
        val blob = cursor.getBlob(2) ?: continue
        val hashes = VideoFingerprintCodec.decodeFrameHashesBlob(blob)
        if (hashes.isEmpty()) {
          continue
        }
        entries.add(
            ImageContentEntry(
                fileEntryId = cursor.getLong(0),
                fingerprintId = cursor.getLong(1),
                dHash = hashes[0],
                sizeBytes = cursor.getLong(3),
                rawContentFingerprintId =
                    if (cursor.isNull(4)) {
                      null
                    } else {
                      cursor.getLong(4)
                    },
            ),
        )
      }
    }
    return entries
  }

  private fun clusterImageContentEntries(entries: List<ImageContentEntry>): List<ImageContentCluster> {
    if (entries.size < 2) {
      return emptyList()
    }

    val parent = IntArray(entries.size) { it }
    fun find(index: Int): Int {
      var root = index
      while (parent[root] != root) {
        root = parent[root]
      }
      var node = index
      while (parent[node] != node) {
        val next = parent[node]
        parent[node] = root
        node = next
      }
      return root
    }
    fun union(left: Int, right: Int) {
      val rootLeft = find(left)
      val rootRight = find(right)
      if (rootLeft != rootRight) {
        parent[rootRight] = rootLeft
      }
    }

    for (i in entries.indices) {
      for (j in i + 1 until entries.size) {
        if (ImageContentMatcher.matches(entries[i].dHash, entries[j].dHash)) {
          union(i, j)
        }
      }
    }

    val clusters = linkedMapOf<Int, MutableList<ImageContentEntry>>()
    for (index in entries.indices) {
      val root = find(index)
      clusters.getOrPut(root) { mutableListOf() }.add(entries[index])
    }

    return clusters.values
        .filter { it.size >= 2 }
        .map { cluster ->
          val fileEntryIds = cluster.map { it.fileEntryId }
          val sizes = cluster.map { it.sizeBytes }
          val matchKind = matchKindForImageCluster(cluster)
          ImageContentCluster(
              candidate =
                  FingerprintGroupCandidate(
                      fingerprintId = cluster.first().fingerprintId,
                      normalizationProfile = NormalizationProfile.IMAGE_CONTENT_V1,
                      fileEntryIds = fileEntryIds,
                      memberCount = fileEntryIds.size,
                      reclaimableBytesEst = estimateReclaimableBytes(sizes),
                  ),
              matchKind = matchKind,
          )
        }
  }

  /** AC-equiv-image-content-04: byte-identical only → `EXACT_BYTES`; mixed encodings → `SAME_CONTENT_IMAGE`. */
  private fun matchKindForImageCluster(cluster: List<ImageContentEntry>): String {
    val rawIds = cluster.map { it.rawContentFingerprintId }
    return if (rawIds.all { it != null } && rawIds.toSet().size == 1) {
      MatchKind.EXACT_BYTES
    } else {
      MatchKind.SAME_CONTENT_IMAGE
    }
  }

  private fun aggregateCandidates(db: SQLiteDatabase, sql: String): List<FingerprintGroupCandidate> {
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
          MatchKind.SAME_CONTENT_IMAGE -> MatchKind.CONFIDENCE_IMAGE_CONTENT
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
