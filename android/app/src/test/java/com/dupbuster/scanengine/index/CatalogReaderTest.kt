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

  @Test
  fun buildMemberPaths_ordersPrimaryThenAliases() {
    val paths =
        reader.buildMemberPaths(
            fileEntryId = 1L,
            primaryUriOrPath = "content://primary",
            aliasesByFileEntryId = mapOf(1L to listOf("content://alias", "content://primary")),
        )
    assertEquals(listOf("content://primary", "content://alias"), paths)
  }

  @Test
  fun readSnapshot_hardLinkMember_exposesMultiplePaths() {
    val uriA =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3Aa.jpg",
        )
    val uriB =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3Ab.jpg",
        )
    val uriAlias =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3Aa-alias.jpg",
        )
    val inodeA = 42L
    val inodeB = 43L
    val device = 3L

    writer.upsertHashed(
        HashedFile(
            staged(uriA, 200, inode = inodeA, deviceId = device),
            "hash-link",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(
            staged(uriB, 200, inode = inodeB, deviceId = device),
            "hash-link",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(
            staged(uriAlias, 200, inode = inodeA, deviceId = device),
            "hash-link",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )

    grouper.rebuildDuplicateGroups()
    val snapshot = reader.readSnapshot()
    val hardLinkMember =
        snapshot.groupDetailsById.values
            .flatMap { it.members }
            .firstOrNull { it.paths.size >= 2 }
    requireNotNull(hardLinkMember) { "expected a catalog member with hard-link aliases" }
    assertEquals(2, hardLinkMember.paths.size)
    assertTrue(hardLinkMember.paths.contains(uriA.toString()))
    assertTrue(hardLinkMember.paths.contains(uriAlias.toString()))
  }

  private fun staged(
      uri: Uri,
      sizeBytes: Long = 100L,
      inode: Long? = null,
      deviceId: Long? = null,
  ): StagedFile {
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "photo-${uri.lastPathSegment}",
            mediaTypeHint = MediaTypeHint.IMAGE,
            sizeBytes = sizeBytes,
            mtimeNs = 2_000_000_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = inode,
        deviceId = deviceId,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.IMAGE,
    )
  }

  private fun staged(uriSuffix: String, sizeBytes: Long = 100L): StagedFile {
    return staged(Uri.parse("content://test/tree/docs/$uriSuffix"), sizeBytes)
  }
}
