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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GrouperVideoContentTest {

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
            uriOrGrant = "content://test/tree",
            mode = com.dupbuster.scanengine.security.ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun rebuildDuplicateGroups_crossResolution_usesSameContentVideoForAllThree() {
    val base = longArrayOf(10, 20, 30, 40, 50)
    val res480 = longArrayOf(11, 21, 31, 41, 51)
    val res640 = longArrayOf(12, 22, 32, 42, 52)

    upsertVideoDual("480-a.mp4", rawHash = "raw-480-a", frameHashes = res480, durationMs = 60_000)
    upsertVideoDual("480-b.mp4", rawHash = "raw-480-b", frameHashes = res480, durationMs = 60_000)
    upsertVideoDual("640.mp4", rawHash = "raw-640", frameHashes = res640, durationMs = 60_200)

    grouper.rebuildDuplicateGroups()

    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(MatchKind.SAME_CONTENT_VIDEO, grouper.matchKindForGroup(groupId))
    assertEquals(3, grouper.memberCountForGroup(groupId))
    assertEquals(1, grouper.duplicateGroupCount())
  }

  @Test
  fun rebuildDuplicateGroups_byteIdenticalVideoPair_exactBytesOnly() {
    val base = longArrayOf(10, 20, 30, 40, 50)
    upsertVideoDual("a.mp4", rawHash = "same-raw", frameHashes = base, durationMs = 60_000)
    upsertVideoDual("b.mp4", rawHash = "same-raw", frameHashes = base, durationMs = 60_000)

    grouper.rebuildDuplicateGroups()

    val groupId = requireNotNull(grouper.firstDuplicateGroupId())
    assertEquals(MatchKind.EXACT_BYTES, grouper.matchKindForGroup(groupId))
    assertEquals(2, grouper.memberCountForGroup(groupId))
  }

  private fun upsertVideoDual(
      suffix: String,
      rawHash: String,
      frameHashes: LongArray,
      durationMs: Long,
  ) {
    val staged = staged(suffix, durationMs = durationMs)
    val blob = VideoFingerprintCodec.encodeFrameHashesBlob(frameHashes)
    val videoHash = VideoFingerprintCodec.canonicalHashValue(frameHashes)
    writer.upsertVideoDualHashed(
        rawBytes = HashedFile(staged, rawHash, NormalizationProfile.RAW_BYTES),
        videoContent =
            HashedFile(
                staged = staged,
                hashValue = videoHash,
                normalizationProfile = NormalizationProfile.VIDEO_CONTENT_V1,
                frameHashesBlob = blob,
            ),
        generation = 1,
    )
  }

  private fun staged(suffix: String, durationMs: Long): StagedFile {
    val uri = Uri.parse("content://test/document/$suffix")
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = suffix,
            mediaTypeHint = MediaTypeHint.VIDEO,
            sizeBytes = 1_000_000L,
            mtimeNs = 1_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = entry.sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = null,
        deviceId = null,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.VIDEO,
        durationMs = durationMs,
        videoWidth = 1280,
        videoHeight = 720,
    )
  }
}
