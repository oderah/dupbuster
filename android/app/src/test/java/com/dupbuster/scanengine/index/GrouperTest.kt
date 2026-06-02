package com.dupbuster.scanengine.index

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.hash.VideoFingerprintCodec
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.stat.StagedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GrouperTest {

  private lateinit var writer: IndexWriter
  private lateinit var grouper: Grouper
  private var rootId: Long = 0

  @Before
  fun setUp() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val database = CatalogDatabase.inMemory(context)
    database.writable()
    writer = IndexWriter(database)
    grouper = Grouper(database)
    rootId =
        writer.insertScanRoot(
            uriOrGrant = "content://test/tree/docs",
            mode = ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun rebuildDuplicateGroups_twoFilesSameFingerprint_createsOneGroup() {
    val hash = "abc123"
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "a"), hash, NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "b"), hash, NormalizationProfile.RAW_BYTES),
        generation = 1,
    )

    val result = grouper.rebuildDuplicateGroups()

    assertEquals(1, result.groupsCreated)
    assertEquals(1, grouper.duplicateGroupCount())
    assertEquals(100L, result.totalReclaimableBytesEst)
    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(2, grouper.memberCountForGroup(groupId))
    assertEquals(100L, grouper.reclaimableBytesForGroup(groupId))
    assertEquals(MatchKind.EXACT_BYTES, grouper.matchKindForGroup(groupId))
  }

  @Test
  fun rebuildDuplicateGroups_threeMembers_reclaimableSumMinusMax() {
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "a", sizeBytes = 100), "h", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "b", sizeBytes = 200), "h", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "c", sizeBytes = 300), "h", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )

    val result = grouper.rebuildDuplicateGroups()

    assertEquals(1, result.groupsCreated)
    assertEquals(300L, result.totalReclaimableBytesEst)
    assertEquals(
        300L,
        grouper.reclaimableBytesForGroup(requireNotNull(grouper.firstDuplicateGroupId())),
    )
  }

  @Test
  fun rebuildDuplicateGroups_uniqueHashes_noGroups() {
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "a"), "h1", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "b"), "h2", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )

    val result = grouper.rebuildDuplicateGroups()

    assertEquals(0, result.groupsCreated)
    assertEquals(0, grouper.duplicateGroupCount())
  }

  @Test
  fun rebuildDuplicateGroups_emptyProfile_groupsAllZeroByteFiles() {
    val emptyHash = NormalizationProfile.EMPTY
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "z1", sizeBytes = 0), emptyHash, NormalizationProfile.EMPTY),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "z2", sizeBytes = 0), emptyHash, NormalizationProfile.EMPTY),
        generation = 1,
    )

    grouper.rebuildDuplicateGroups()

    assertEquals(1, grouper.duplicateGroupCount())
    assertEquals(
        0L,
        grouper.reclaimableBytesForGroup(requireNotNull(grouper.firstDuplicateGroupId())),
    )
  }

  @Test
  fun rebuildDuplicateGroups_textNfcLfProfile_exactBytesMatchKind() {
    val hash = "texthash"
    writer.upsertHashed(
        HashedFile(
            staged(uriSuffix = "t1", mediaTypeHint = MediaTypeHint.TEXT),
            hash,
            NormalizationProfile.TEXT_NFC_LF,
        ),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(
            staged(uriSuffix = "t2", mediaTypeHint = MediaTypeHint.TEXT),
            hash,
            NormalizationProfile.TEXT_NFC_LF,
        ),
        generation = 1,
    )

    grouper.rebuildDuplicateGroups()

    assertEquals(
        MatchKind.EXACT_BYTES,
        grouper.matchKindForGroup(requireNotNull(grouper.firstDuplicateGroupId())),
    )
  }

  @Test
  fun rebuildDuplicateGroups_videoProfile_sameContentVideoMatchKind() {
    val frameHashes = longArrayOf(10, 20, 30, 40, 50)
    val blob = VideoFingerprintCodec.encodeFrameHashesBlob(frameHashes)
    val videoHash = VideoFingerprintCodec.canonicalHashValue(frameHashes)
    val stagedA = staged(uriSuffix = "v3", mediaTypeHint = MediaTypeHint.VIDEO, durationMs = 60_000)
    val stagedB = staged(uriSuffix = "v4", mediaTypeHint = MediaTypeHint.VIDEO, durationMs = 60_000)
    writer.upsertVideoDualHashed(
        rawBytes = HashedFile(stagedA, "raw-a", NormalizationProfile.RAW_BYTES),
        videoContent =
            HashedFile(
                staged = stagedA,
                hashValue = videoHash,
                normalizationProfile = NormalizationProfile.VIDEO_CONTENT_V1,
                frameHashesBlob = blob,
            ),
        generation = 1,
    )
    writer.upsertVideoDualHashed(
        rawBytes = HashedFile(stagedB, "raw-b", NormalizationProfile.RAW_BYTES),
        videoContent =
            HashedFile(
                staged = stagedB,
                hashValue = videoHash,
                normalizationProfile = NormalizationProfile.VIDEO_CONTENT_V1,
                frameHashesBlob = blob,
            ),
        generation = 1,
    )

    grouper.rebuildDuplicateGroups()

    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(MatchKind.SAME_CONTENT_VIDEO, grouper.matchKindForGroup(groupId))
  }

  @Test
  fun rebuildDuplicateGroups_excludesSymlinksAndUnscannable() {
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "ok1"), "h", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(uriSuffix = "ok2"), "h", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertSymlink(staged(uriSuffix = "link", isSymlink = true), generation = 1)
    writer.upsertUnscannable(
        "LARGE_SKIPPED",
        staged(uriSuffix = "big", sizeBytes = 9_000),
        generation = 1,
    )

    grouper.rebuildDuplicateGroups()

    assertEquals(1, grouper.duplicateGroupCount())
    assertEquals(2, grouper.memberCountForGroup(requireNotNull(grouper.firstDuplicateGroupId())))
  }

  @Test
  fun estimateReclaimableBytes_matchesFrAc06Preview() {
    assertEquals(0L, Grouper.estimateReclaimableBytes(listOf(100L)))
    assertEquals(100L, Grouper.estimateReclaimableBytes(listOf(100L, 200L)))
    assertEquals(300L, Grouper.estimateReclaimableBytes(listOf(100L, 200L, 300L)))
  }

  @Test
  fun matchKindForProfile_mapsVideoContent() {
    assertEquals(MatchKind.EXACT_BYTES, Grouper.matchKindForProfile(NormalizationProfile.RAW_BYTES))
    assertEquals(
        MatchKind.SAME_CONTENT_VIDEO,
        Grouper.matchKindForProfile(NormalizationProfile.VIDEO_CONTENT_V1),
    )
  }

  private fun staged(
      uriSuffix: String,
      sizeBytes: Long = 100L,
      mediaTypeHint: MediaTypeHint = MediaTypeHint.OTHER,
      isSymlink: Boolean = false,
      durationMs: Long = 0L,
  ): StagedFile {
    val uri = Uri.parse("content://test/document/$uriSuffix")
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "$uriSuffix.bin",
            mediaTypeHint = mediaTypeHint,
            sizeBytes = sizeBytes,
            mtimeNs = 1_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = null,
        deviceId = null,
        isSymlink = isSymlink,
        mediaTypeHint = mediaTypeHint,
        durationMs = durationMs,
    )
  }
}
