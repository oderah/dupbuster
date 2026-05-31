package com.dupbuster.scanengine.index

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.index.MatchKind
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.stat.StagedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CatalogReaderTest {

  private lateinit var writer: IndexWriter
  private lateinit var grouper: Grouper
  private lateinit var reader: CatalogReader
  private var rootId: Long = 0

  @Before
  fun setUp() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val database = CatalogDatabase.inMemory(context)
    database.writable()
    writer = IndexWriter(database)
    grouper = Grouper(database)
    reader = CatalogReader(database)
    rootId =
        writer.insertScanRoot(
            uriOrGrant = "content://test/tree/docs",
            mode = ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun readSnapshot_emptyCatalog_returnsEmptyStructures() {
    val snapshot = reader.readSnapshot()

    assertTrue(snapshot.duplicateGroups.isEmpty())
    assertTrue(snapshot.unscannableCounts.isEmpty())
    assertTrue(snapshot.groupDetailsById.isEmpty())
  }

  @Test
  fun readSnapshot_afterGrouping_returnsGroupsMembersAndUnscannableCounts() {
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "a.jpg", sizeBytes = 100), "hash-a", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "b.jpg", sizeBytes = 100), "hash-a", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertUnscannable(
        "HASH_TIMEOUT",
        staged(uriSuffix = "timeout.bin", sizeBytes = 50),
        generation = 1,
    )
    writer.upsertUnscannable(
        "LARGE_SKIPPED",
        staged(uriSuffix = "large.bin", sizeBytes = 50),
        generation = 1,
    )

    grouper.rebuildDuplicateGroups()
    val snapshot = reader.readSnapshot()

    assertEquals(1, snapshot.duplicateGroups.size)
    val summary = snapshot.duplicateGroups.first()
    assertEquals(MatchKind.EXACT_BYTES, summary.matchKind)
    assertEquals(2, summary.memberCount)
    assertEquals(100L, summary.reclaimableBytesEst)
    assertEquals(2, summary.thumbnails.size)

    val detail = requireNotNull(snapshot.groupDetailsById[summary.groupId])
    assertEquals(2, detail.members.size)
    assertEquals("photo-a.jpg", detail.members.first().displayName)
    assertEquals(MediaTypeHint.IMAGE, detail.members.first().mediaTypeHint)
    assertNotNull(detail.members.first().thumbnailUri)
    assertEquals(2, snapshot.unscannableCounts.size)
    assertEquals(1, snapshot.unscannableCounts["HASH_TIMEOUT"])
    assertEquals(1, snapshot.unscannableCounts["LARGE_SKIPPED"])
  }

  @Test
  fun resolveMediaTypeHint_durationMs_prefersVideo() {
    assertEquals(
        MediaTypeHint.VIDEO,
        reader.resolveMediaTypeHint(displayName = "clip.bin", durationMs = 1000L),
    )
  }

  @Test
  fun thumbnailUriFor_nonVisualMedia_returnsNull() {
    assertNull(reader.thumbnailUriFor(MediaTypeHint.TEXT, "content://test/doc.txt"))
  }

  private fun staged(uriSuffix: String, sizeBytes: Long = 100L): StagedFile {
    val uri = Uri.parse("content://test/tree/docs/$uriSuffix")
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "photo-$uriSuffix",
            mediaTypeHint = MediaTypeHint.IMAGE,
            sizeBytes = sizeBytes,
            mtimeNs = 2_000_000_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = null,
        deviceId = null,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.IMAGE,
    )
  }
}
