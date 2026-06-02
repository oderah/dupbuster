package com.dupbuster.scanengine.bridge

import com.dupbuster.scanengine.index.CatalogReader
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

/** Maps [CatalogReader.CatalogSnapshot] to the RN `getCatalogSnapshot` payload. */
object CatalogSnapshotBridgeMapper {

  val ALLOWED_TOP_LEVEL_KEYS: Set<String> =
      setOf("duplicateGroups", "unscannableCounts", "groupDetailsById")

  val ALLOWED_GROUP_SUMMARY_KEYS: Set<String> =
      setOf("groupId", "matchKind", "memberCount", "reclaimableBytesEst", "thumbnails")

  val ALLOWED_THUMBNAIL_KEYS: Set<String> = setOf("fileEntryId", "mediaTypeHint", "thumbnailUri")

  val ALLOWED_GROUP_DETAIL_KEYS: Set<String> =
      setOf("groupId", "matchKind", "memberCount", "reclaimableBytesEst", "members")

  val ALLOWED_MEMBER_KEYS: Set<String> =
      setOf(
          "fileEntryId",
          "displayName",
          "sizeBytes",
          "mtimeMs",
          "pathLength",
          "mediaTypeHint",
          "thumbnailUri",
          "paths",
      )

  fun toReadableMap(snapshot: CatalogReader.CatalogSnapshot): ReadableMap {
    val map =
        JavaOnlyMap().apply {
          putArray("duplicateGroups", duplicateGroupsArray(snapshot.duplicateGroups))
          putMap("unscannableCounts", unscannableCountsMap(snapshot.unscannableCounts))
          putMap("groupDetailsById", groupDetailsMap(snapshot.groupDetailsById))
        }
    assertBridgeSafePayload(map)
    return map
  }

  fun assertBridgeSafePayload(map: ReadableMap) {
    assertKeys(map, ALLOWED_TOP_LEVEL_KEYS)
    val duplicateGroups = map.getArray("duplicateGroups")
    checkNotNull(duplicateGroups) { "duplicateGroups required" }
    for (index in 0 until duplicateGroups.size()) {
      val group = duplicateGroups.getMap(index)
      checkNotNull(group) { "duplicateGroups[$index] required" }
      assertKeys(group, ALLOWED_GROUP_SUMMARY_KEYS)
      val thumbnails = group.getArray("thumbnails")
      checkNotNull(thumbnails) { "thumbnails required" }
      for (thumbIndex in 0 until thumbnails.size()) {
        val thumbnail = thumbnails.getMap(thumbIndex)
        checkNotNull(thumbnail) { "thumbnails[$thumbIndex] required" }
        assertKeys(thumbnail, ALLOWED_THUMBNAIL_KEYS)
      }
    }
    val unscannableCounts = map.getMap("unscannableCounts")
    checkNotNull(unscannableCounts) { "unscannableCounts required" }
    val groupDetailsById = map.getMap("groupDetailsById")
    checkNotNull(groupDetailsById) { "groupDetailsById required" }
    val detailIterator = groupDetailsById.keySetIterator()
    while (detailIterator.hasNextKey()) {
      val detail = groupDetailsById.getMap(detailIterator.nextKey())
      checkNotNull(detail) { "group detail required" }
      assertKeys(detail, ALLOWED_GROUP_DETAIL_KEYS)
      val members = detail.getArray("members")
      checkNotNull(members) { "members required" }
      for (memberIndex in 0 until members.size()) {
        val member = members.getMap(memberIndex)
        checkNotNull(member) { "members[$memberIndex] required" }
        assertKeys(member, ALLOWED_MEMBER_KEYS)
      }
    }
  }

  private fun duplicateGroupsArray(groups: List<CatalogReader.CatalogGroupSummary>): WritableArray {
    val array = JavaOnlyArray()
    for (group in groups) {
      array.pushMap(
          JavaOnlyMap().apply {
            putDouble("groupId", group.groupId.toDouble())
            putString("matchKind", group.matchKind)
            putDouble("memberCount", group.memberCount.toDouble())
            putDouble("reclaimableBytesEst", group.reclaimableBytesEst.toDouble())
            putArray("thumbnails", thumbnailsArray(group.thumbnails))
          },
      )
    }
    return array
  }

  private fun groupDetailsMap(
      detailsById: Map<Long, CatalogReader.CatalogGroupDetail>,
  ): WritableMap {
    val map = JavaOnlyMap()
    for ((groupId, detail) in detailsById) {
      map.putMap(
          groupId.toString(),
          JavaOnlyMap().apply {
            putDouble("groupId", detail.groupId.toDouble())
            putString("matchKind", detail.matchKind)
            putDouble("memberCount", detail.memberCount.toDouble())
            putDouble("reclaimableBytesEst", detail.reclaimableBytesEst.toDouble())
            putArray("members", membersArray(detail.members))
          },
      )
    }
    return map
  }

  private fun thumbnailsArray(members: List<CatalogReader.CatalogMember>): WritableArray {
    val array = JavaOnlyArray()
    for (member in members) {
      array.pushMap(thumbnailMap(member))
    }
    return array
  }

  private fun thumbnailMap(member: CatalogReader.CatalogMember): WritableMap {
    return JavaOnlyMap().apply {
      putDouble("fileEntryId", member.fileEntryId.toDouble())
      putString("mediaTypeHint", member.mediaTypeHint.wireValue)
      if (member.thumbnailUri != null) {
        putString("thumbnailUri", member.thumbnailUri)
      }
    }
  }

  private fun membersArray(members: List<CatalogReader.CatalogMember>): WritableArray {
    val array = JavaOnlyArray()
    for (member in members) {
      array.pushMap(memberMap(member))
    }
    return array
  }

  private fun memberMap(member: CatalogReader.CatalogMember): WritableMap {
    return JavaOnlyMap().apply {
      putDouble("fileEntryId", member.fileEntryId.toDouble())
      putString("displayName", member.displayName)
      putDouble("sizeBytes", member.sizeBytes.toDouble())
      putDouble("mtimeMs", member.mtimeMs.toDouble())
      putDouble("pathLength", member.pathLength.toDouble())
      putString("mediaTypeHint", member.mediaTypeHint.wireValue)
      if (member.thumbnailUri != null) {
        putString("thumbnailUri", member.thumbnailUri)
      }
      putArray("paths", pathsArray(member.paths))
    }
  }

  private fun pathsArray(paths: List<String>): WritableArray {
    val array = JavaOnlyArray()
    for (path in paths) {
      array.pushString(path)
    }
    return array
  }

  private fun unscannableCountsMap(counts: Map<String, Int>): WritableMap {
    val map = JavaOnlyMap()
    for ((reason, count) in counts) {
      map.putDouble(reason, count.toDouble())
    }
    return map
  }

  private fun assertKeys(map: ReadableMap, allowedKeys: Set<String>) {
    val iterator = map.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      check(key in allowedKeys) { "Forbidden bridge field: $key" }
    }
  }
}
