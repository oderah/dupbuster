package com.dupbuster.scanengine.bridge

import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.index.CatalogReader
import com.dupbuster.scanengine.index.MatchKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CatalogSnapshotBridgeMapperTest {

  @Test
  fun toReadableMap_mapsSnapshotShape() {
    val member =
        CatalogReader.CatalogMember(
            fileEntryId = 10L,
            displayName = "photo.jpg",
            sizeBytes = 100L,
            mtimeMs = 2L,
            pathLength = 24,
            mediaTypeHint = MediaTypeHint.IMAGE,
            thumbnailUri = "content://test/photo.jpg",
            paths = listOf("content://test/photo.jpg"),
        )
    val snapshot =
        CatalogReader.CatalogSnapshot(
            duplicateGroups =
                listOf(
                    CatalogReader.CatalogGroupSummary(
                        groupId = 1L,
                        matchKind = MatchKind.EXACT_BYTES,
                        memberCount = 1,
                        reclaimableBytesEst = 0L,
                        thumbnails = listOf(member),
                    ),
                ),
            unscannableCounts = mapOf("HASH_TIMEOUT" to 2),
            groupDetailsById =
                mapOf(
                    1L to
                        CatalogReader.CatalogGroupDetail(
                            groupId = 1L,
                            matchKind = MatchKind.EXACT_BYTES,
                            memberCount = 1,
                            reclaimableBytesEst = 0L,
                            members = listOf(member),
                        ),
                ),
        )

    val map = CatalogSnapshotBridgeMapper.toReadableMap(snapshot)

    assertEquals(1, map.getArray("duplicateGroups")!!.size())
    assertEquals(2.0, map.getMap("unscannableCounts")!!.getDouble("HASH_TIMEOUT"), 0.0)
    val detail = map.getMap("groupDetailsById")!!.getMap("1")!!
    assertEquals(1, detail.getArray("members")!!.size())
    assertEquals("photo.jpg", detail.getArray("members")!!.getMap(0)!!.getString("displayName"))
    assertEquals(
        "content://test/photo.jpg",
        detail.getArray("members")!!.getMap(0)!!.getArray("paths")!!.getString(0),
    )
  }
}
