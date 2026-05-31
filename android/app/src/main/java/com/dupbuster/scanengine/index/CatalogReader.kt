package com.dupbuster.scanengine.index

import android.database.Cursor
import com.dupbuster.scanengine.discovery.MediaTypeHint

/**
 * Reads duplicate groups and unscannable aggregates from SQLite for the catalog bridge (Phase B).
 * Not used on the progress/error event stream.
 */
class CatalogReader(private val database: CatalogDatabase) {

  data class CatalogMember(
      val fileEntryId: Long,
      val displayName: String,
      val sizeBytes: Long,
      val mtimeMs: Long,
      val pathLength: Int,
      val mediaTypeHint: MediaTypeHint,
      val thumbnailUri: String?,
  )

  data class CatalogGroupSummary(
      val groupId: Long,
      val matchKind: String,
      val memberCount: Int,
      val reclaimableBytesEst: Long,
      val thumbnails: List<CatalogMember>,
  )

  data class CatalogGroupDetail(
      val groupId: Long,
      val matchKind: String,
      val memberCount: Int,
      val reclaimableBytesEst: Long,
      val members: List<CatalogMember>,
  )

  data class CatalogSnapshot(
      val duplicateGroups: List<CatalogGroupSummary>,
      val unscannableCounts: Map<String, Int>,
      val groupDetailsById: Map<Long, CatalogGroupDetail>,
  )

  fun readSnapshot(): CatalogSnapshot {
    val groupRows = loadGroupRows()
    val membersByGroupId = loadMembersByGroupId()
    val duplicateGroups =
        groupRows.map { row ->
          val members = membersByGroupId[row.groupId].orEmpty()
          CatalogGroupSummary(
              groupId = row.groupId,
              matchKind = row.matchKind,
              memberCount = row.memberCount,
              reclaimableBytesEst = row.reclaimableBytesEst,
              thumbnails = members.take(THUMBNAIL_PREVIEW_LIMIT),
          )
        }
    val groupDetailsById =
        groupRows.associate { row ->
          val members = membersByGroupId[row.groupId].orEmpty()
          row.groupId to
              CatalogGroupDetail(
                  groupId = row.groupId,
                  matchKind = row.matchKind,
                  memberCount = row.memberCount,
                  reclaimableBytesEst = row.reclaimableBytesEst,
                  members = members,
              )
        }
    return CatalogSnapshot(
        duplicateGroups = duplicateGroups,
        unscannableCounts = loadUnscannableCounts(),
        groupDetailsById = groupDetailsById,
    )
  }

  private data class GroupRow(
      val groupId: Long,
      val matchKind: String,
      val memberCount: Int,
      val reclaimableBytesEst: Long,
  )

  private fun loadGroupRows(): List<GroupRow> {
    val rows = mutableListOf<GroupRow>()
    database
        .readable()
        .rawQuery(
            """
            SELECT id, match_kind, member_count, reclaimable_bytes_est
            FROM duplicate_group
            ORDER BY id ASC
            """
                .trimIndent(),
            null,
        )
        .use { cursor ->
          while (cursor.moveToNext()) {
            rows.add(
                GroupRow(
                    groupId = cursor.getLong(0),
                    matchKind = cursor.getString(1),
                    memberCount = cursor.getInt(2),
                    reclaimableBytesEst = cursor.getLong(3),
                ),
            )
          }
        }
    return rows
  }

  private fun loadMembersByGroupId(): Map<Long, List<CatalogMember>> {
    val membersByGroupId = linkedMapOf<Long, MutableList<CatalogMember>>()
    database
        .readable()
        .rawQuery(
            """
            SELECT dm.group_id,
                   fe.id,
                   fe.display_name,
                   fe.uri_or_path,
                   fe.size,
                   fe.mtime_ns,
                   fe.duration_ms
            FROM duplicate_member dm
            INNER JOIN file_entry fe ON fe.id = dm.file_entry_id
            ORDER BY dm.group_id ASC, fe.id ASC
            """
                .trimIndent(),
            null,
        )
        .use { cursor ->
          while (cursor.moveToNext()) {
            val groupId = cursor.getLong(0)
            val member = rowToCatalogMember(cursor, startColumn = 1)
            membersByGroupId.getOrPut(groupId) { mutableListOf() }.add(member)
          }
        }
    return membersByGroupId
  }

  private fun loadUnscannableCounts(): Map<String, Int> {
    val counts = linkedMapOf<String, Int>()
    database
        .readable()
        .rawQuery(
            """
            SELECT unscannable_reason, COUNT(*)
            FROM file_entry
            WHERE unscannable_reason IS NOT NULL
            GROUP BY unscannable_reason
            ORDER BY unscannable_reason ASC
            """
                .trimIndent(),
            null,
        )
        .use { cursor ->
          while (cursor.moveToNext()) {
            counts[cursor.getString(0)] = cursor.getInt(1)
          }
        }
    return counts
  }

  private fun rowToCatalogMember(cursor: Cursor, startColumn: Int): CatalogMember {
    val displayName = cursor.getString(startColumn + 1)
    val uriOrPath = cursor.getString(startColumn + 2)
    val durationMs =
        if (cursor.isNull(startColumn + 5)) {
          0L
        } else {
          cursor.getLong(startColumn + 5)
        }
    val mediaTypeHint = resolveMediaTypeHint(displayName, durationMs)
    return CatalogMember(
        fileEntryId = cursor.getLong(startColumn),
        displayName = displayName,
        sizeBytes = cursor.getLong(startColumn + 3),
        mtimeMs = cursor.getLong(startColumn + 4) / 1_000_000L,
        pathLength = uriOrPath.length,
        mediaTypeHint = mediaTypeHint,
        thumbnailUri = thumbnailUriFor(mediaTypeHint, uriOrPath),
    )
  }

  internal fun resolveMediaTypeHint(displayName: String, durationMs: Long): MediaTypeHint {
    if (durationMs > 0L) {
      return MediaTypeHint.VIDEO
    }
    return MediaTypeHint.fromFileName(displayName)
  }

  internal fun thumbnailUriFor(mediaTypeHint: MediaTypeHint, uriOrPath: String): String? {
    if (mediaTypeHint != MediaTypeHint.IMAGE && mediaTypeHint != MediaTypeHint.VIDEO) {
      return null
    }
    return uriOrPath
  }

  companion object {
    const val THUMBNAIL_PREVIEW_LIMIT: Int = 4
  }
}
