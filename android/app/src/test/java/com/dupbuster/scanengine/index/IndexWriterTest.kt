package com.dupbuster.scanengine.index

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashResult
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.hash.SizeBucketDisposition
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.security.UnscannableReason
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
class IndexWriterTest {

  private lateinit var context: Context
  private lateinit var database: CatalogDatabase
  private lateinit var writer: IndexWriter
  private var rootId: Long = 0

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = CatalogDatabase.inMemory(context)
    database.writable()
    writer = IndexWriter(database)
    rootId =
        writer.insertScanRoot(
            uriOrGrant = "content://test/tree/docs",
            mode = ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun readCatalogMeta_returnsSchemaVersionFromMetaTable() {
    val meta = writer.readCatalogMeta()
    assertEquals(CatalogSchema.CURRENT_VERSION, meta.schemaVersion)
    assertEquals(false, meta.fullRescanRequired)
  }

  @Test
  fun upsertHashed_createsFingerprintAndFileEntry() {
    val staged = staged(sizeBytes = 100, mediaTypeHint = MediaTypeHint.OTHER)
    val hashed =
        HashedFile(
            staged = staged,
            hashValue = "abc123",
            normalizationProfile = NormalizationProfile.RAW_BYTES,
        )

    val fileEntryId = writer.upsertHashed(hashed, generation = 1)

    assertTrue(fileEntryId > 0)
    assertEquals(1, writer.fileEntryCount())
    assertNotNull(writer.findFingerprintId("abc123", NormalizationProfile.RAW_BYTES))
  }

  @Test
  fun upsertHashed_sameHashProfile_reusesFingerprint() {
    val hash = "deadbeef"
    writer.upsertHashed(
        HashedFile(staged(stagedUri("a"), 10), hash, NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(stagedUri("b"), 20), hash, NormalizationProfile.RAW_BYTES),
        generation = 1,
    )

    assertNotNull(writer.findFingerprintId(hash, NormalizationProfile.RAW_BYTES))
    assertEquals(2, writer.fileEntryCount())
  }

  @Test
  fun upsertVideoPartialHashed_persistsVideoDecodeFailedReason() {
    val staged = staged(sizeBytes = 100, mediaTypeHint = MediaTypeHint.VIDEO).copy(durationMs = 5_000)
    val raw =
        HashedFile(
            staged = staged,
            hashValue = "rawonly",
            normalizationProfile = NormalizationProfile.RAW_BYTES,
        )
    val id =
        writer.upsertVideoPartialHashed(
            rawBytes = raw,
            videoUnscannableReason = UnscannableReason.VIDEO_DECODE_FAILED,
            generation = 1,
        )

    assertTrue(id > 0)
    assertEquals(UnscannableReason.VIDEO_DECODE_FAILED, writer.unscannableReasonForEntry(id))
    assertNotNull(writer.findFingerprintId("rawonly", NormalizationProfile.RAW_BYTES))
  }

  @Test
  fun upsertUnscannable_hasNullFingerprint() {
    val staged = staged(sizeBytes = 50)
    val id =
        writer.upsertUnscannable(UnscannableReason.LARGE_SKIPPED, staged, generation = 1)

    assertTrue(id > 0)
    assertNull(writer.findFingerprintId("anything", NormalizationProfile.RAW_BYTES))
  }

  @Test
  fun upsertSymlink_setsIsSymlinkFlag() {
    val staged = staged(sizeBytes = 10, isSymlink = true)
    writer.upsertSymlink(staged, generation = 1)
    assertEquals(1, writer.fileEntryCount())
  }

  @Test
  fun hardLink_sameInodeDevice_oneFileEntryTwoAliases() {
    val uriA =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3Aa.txt",
        )
    val uriB =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3Ab.txt",
        )
    val inode = 99L
    val device = 7L

    writer.upsertHashed(
        HashedFile(
            staged(uriA, 100, inode = inode, deviceId = device),
            "samehash",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(
            staged(uriB, 100, inode = inode, deviceId = device),
            "samehash",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )

    assertEquals(1, writer.fileEntryCount())
    assertEquals(1, aliasCount())
  }

  @Test
  fun countIndexedFilesWithSize_excludesVideoRows() {
    val size = 2048L
    writer.upsertHashed(
        HashedFile(
            staged(sizeBytes = size, mediaTypeHint = MediaTypeHint.VIDEO).copy(durationMs = 60_000),
            "video-hash",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )

    assertEquals(0, writer.countIndexedFilesWithSize(size))
    val index = SqliteSizeBucketIndex(writer)
    val disposition = index.register(size, MediaTypeHint.OTHER, isEmpty = false)
    assertEquals(SizeBucketDisposition.UNIQUE_SKIP, disposition)
  }

  @Test
  fun countIndexedFilesWithSize_drivesSizeBucketSkip() {
    val size = 2048L
    writer.upsertSizeBucketSkipped(staged(sizeBytes = size), generation = 1)

    assertEquals(1, writer.countIndexedFilesWithSize(size))
    val index = SqliteSizeBucketIndex(writer)
    val disposition = index.register(size, MediaTypeHint.OTHER, isEmpty = false)
    assertEquals(SizeBucketDisposition.NEEDS_HASH, disposition)
  }

  @Test
  fun countVideosWithinDurationGate_findsMatchingDuration() {
    writer.upsertHashed(
        HashedFile(
            staged(sizeBytes = 100, mediaTypeHint = MediaTypeHint.VIDEO).copy(durationMs = 60_000),
            "v1",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )

    assertEquals(1, writer.countVideosWithinDurationGate(60_100))
    assertEquals(0, writer.countVideosWithinDurationGate(62_000))
  }

  @Test
  fun purgeEntriesNotSeenInGeneration_removesStaleRows() {
    writer.upsertHashed(
        HashedFile(staged(sizeBytes = 1), "h1", NormalizationProfile.RAW_BYTES),
        generation = 1,
    )
    writer.upsertHashed(
        HashedFile(staged(stagedUri("new"), 2), "h2", NormalizationProfile.RAW_BYTES),
        generation = 2,
    )

    val purged = writer.purgeEntriesNotSeenInGeneration(rootId, generation = 2)
    assertEquals(1, purged)
    assertEquals(1, writer.fileEntryCount())
  }

  @Test
  fun persistHashResult_successPath() {
    val staged = staged(sizeBytes = 8)
    val result =
        HashResult.Success(
            HashedFile(staged, NormalizationProfile.EMPTY, NormalizationProfile.EMPTY),
        )
    val id = writer.persistHashResult(result, staged, generation = 1)
    assertTrue(id > 0)
  }

  private fun aliasCount(): Int {
    val cursor = database.readable().rawQuery("SELECT COUNT(*) FROM file_path", null)
    cursor.use {
      return if (it.moveToFirst()) it.getInt(0) else 0
    }
  }

  private fun staged(
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint = MediaTypeHint.OTHER,
      isSymlink: Boolean = false,
      inode: Long? = null,
      deviceId: Long? = null,
  ): StagedFile = staged(stagedUri("file"), sizeBytes, mediaTypeHint, isSymlink, inode, deviceId)

  private fun staged(
      uri: Uri,
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint = MediaTypeHint.OTHER,
      isSymlink: Boolean = false,
      inode: Long? = null,
      deviceId: Long? = null,
  ): StagedFile {
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "file.bin",
            mediaTypeHint = mediaTypeHint,
            sizeBytes = sizeBytes,
            mtimeNs = 1_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = inode,
        deviceId = deviceId,
        isSymlink = isSymlink,
        mediaTypeHint = mediaTypeHint,
    )
  }

  private fun stagedUri(suffix: String): Uri =
      Uri.parse("content://test/document/$suffix")
}
