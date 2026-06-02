package com.dupbuster.scanengine.index

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.hash.VideoFingerprintCodec
import com.dupbuster.scanengine.stat.StagedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GrouperImageContentTest {

  private lateinit var context: Context
  private lateinit var database: CatalogDatabase
  private lateinit var writer: IndexWriter
  private lateinit var grouper: Grouper
  private var rootId: Long = 0

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = CatalogDatabase.inMemory(context)
    database.writable()
    writer = IndexWriter(database)
    grouper = Grouper(database)
    rootId =
        writer.insertScanRoot(
            uriOrGrant = "content://test/tree",
            mode = com.dupbuster.scanengine.security.ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun rebuildDuplicateGroups_mixedRawBytes_usesSameContentImageForAllThree() {
    val base = 0x0F0E0D0C0B0A0908L
    val recompressed = base xor (1L shl 3)

    upsertImageDual("download.jpeg", rawHash = "raw-small", dHash = recompressed)
    upsertImageDual("images.jpeg", rawHash = "raw-large", dHash = base)
    upsertImageDual("images (2).jpeg", rawHash = "raw-large", dHash = base)

    grouper.rebuildDuplicateGroups()

    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(MatchKind.SAME_CONTENT_IMAGE, grouper.matchKindForGroup(groupId))
    assertEquals(3, grouper.memberCountForGroup(groupId))
    assertEquals(1, grouper.duplicateGroupCount())
  }

  @Test
  fun rebuildDuplicateGroups_byteIdenticalImagePair_exactBytesOnly() {
    val base = 0x0F0E0D0C0B0A0908L
    upsertImageDual("a.jpg", rawHash = "same-raw", dHash = base)
    upsertImageDual("b.jpg", rawHash = "same-raw", dHash = base)

    grouper.rebuildDuplicateGroups()

    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(MatchKind.EXACT_BYTES, grouper.matchKindForGroup(groupId))
    assertEquals(2, grouper.memberCountForGroup(groupId))
  }

  @Test
  fun rebuildDuplicateGroups_fuzzyImageContent_clustersRecompressedVariants() {
    val base = 0x0F0E0D0C0B0A0908L
    val closeA = base xor (1L shl 1)
    val closeB = base xor (1L shl 8)

    upsertImageDual("a.jpg", rawHash = "raw-a", dHash = base)
    upsertImageDual("b.jpg", rawHash = "raw-b", dHash = closeA)
    upsertImageDual("c.jpg", rawHash = "raw-c", dHash = closeB)

    grouper.rebuildDuplicateGroups()

    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(MatchKind.SAME_CONTENT_IMAGE, grouper.matchKindForGroup(groupId))
    assertEquals(3, grouper.memberCountForGroup(groupId))
  }

  private fun upsertImageDual(suffix: String, rawHash: String, dHash: Long) {
    val staged = staged(suffix)
    val blob = VideoFingerprintCodec.encodeFrameHashesBlob(longArrayOf(dHash))
    val imageHash = VideoFingerprintCodec.canonicalHashValue(longArrayOf(dHash))
    writer.upsertImageDualHashed(
        rawBytes = HashedFile(staged, rawHash, NormalizationProfile.RAW_BYTES),
        imageContent =
            HashedFile(
                staged = staged,
                hashValue = imageHash,
                normalizationProfile = NormalizationProfile.IMAGE_CONTENT_V1,
                frameHashesBlob = blob,
            ),
        generation = 1,
    )
  }

  private fun staged(suffix: String): StagedFile {
    val uri = Uri.parse("content://test/document/$suffix")
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = suffix,
            mediaTypeHint = MediaTypeHint.IMAGE,
            sizeBytes = 100L,
            mtimeNs = 1_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = entry.sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = null,
        deviceId = null,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.IMAGE,
    )
  }
}
