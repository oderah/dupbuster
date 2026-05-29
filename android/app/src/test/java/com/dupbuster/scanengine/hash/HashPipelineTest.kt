package com.dupbuster.scanengine.hash

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile
import java.io.ByteArrayInputStream
import java.io.InputStream
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
class HashPipelineTest {

  private lateinit var context: Context
  private val contentUri =
      Uri.parse(
          "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fpayload.bin",
      )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun hash_emptyFile_returnsEmptyProfile() {
    val pipeline = HashPipeline(context, FakeContentReader(byteArrayOf()))
    val result = pipeline.hash(staged(sizeBytes = 0))

    val success = result as HashResult.Success
    assertEquals(NormalizationProfile.EMPTY, success.hashed.hashValue)
    assertEquals(NormalizationProfile.EMPTY, success.hashed.normalizationProfile)
    assertNull(success.hashed.quickSampleHash)
  }

  @Test
  fun hash_smallFile_returnsRawBytesSha256() {
    val payload = "hello".toByteArray(Charsets.UTF_8)
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(payload),
            sizeBucketIndex = alwaysNeedsHashIndex(),
        )
    val result = pipeline.hash(staged(sizeBytes = payload.size.toLong()))

    val success = result as HashResult.Success
    assertEquals(NormalizationProfile.RAW_BYTES, success.hashed.normalizationProfile)
    assertEquals(
        "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
        success.hashed.hashValue,
    )
    assertNull(success.hashed.quickSampleHash)
  }

  @Test
  fun hash_uniqueSize_skipsByteRead() {
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(byteArrayOf(1)),
            sizeBucketIndex = InMemorySizeBucketIndex(),
        )
    val result = pipeline.hash(staged(sizeBytes = 1024))

    assertTrue(result is HashResult.SizeBucketSkipped)
  }

  @Test
  fun hash_duplicateSize_hashesBoth() {
    val payload = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
    val index = InMemorySizeBucketIndex()
    val reader = FakeContentReader(payload)
    val pipeline = HashPipeline(context, reader, sizeBucketIndex = index)

    val first = pipeline.hash(staged(sizeBytes = 2))
    val second = pipeline.hash(staged(sizeBytes = 2))

    assertTrue(first is HashResult.SizeBucketSkipped)
    assertTrue(second is HashResult.Success)
    assertEquals(2, index.count(2))
  }

  @Test
  fun hash_videoUniqueSize_stillHashes() {
    val payload = byteArrayOf(0x01)
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(payload),
            sizeBucketIndex = InMemorySizeBucketIndex(),
        )
    val result =
        pipeline.hash(
            staged(sizeBytes = 1, mediaTypeHint = MediaTypeHint.VIDEO),
        )

    assertTrue(result is HashResult.Success)
  }

  @Test
  fun hash_largeFileWithoutOptIn_returnsLargeSkipped() {
    val pipeline = HashPipeline(context, FakeContentReader(byteArrayOf(1)))
    val overCap = HashConstants.LARGE_FILE_CAP_BYTES + 1
    val result = pipeline.hash(staged(sizeBytes = overCap))

    val unscannable = result as HashResult.Unscannable
    assertEquals(UnscannableReason.LARGE_SKIPPED, unscannable.reason)
  }

  @Test
  fun hash_largeFileWithOptIn_proceedsWhenSizeCollides() {
    val payload = byteArrayOf(0x01, 0x02)
    val index = InMemorySizeBucketIndex()
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(payload),
            sizeBucketIndex = index,
        )
    val overCap = HashConstants.LARGE_FILE_CAP_BYTES + 1
    val settings = HashSettings(largeFilesOptIn = true)

    pipeline.hash(staged(sizeBytes = overCap), settings)
    val second = pipeline.hash(staged(sizeBytes = overCap), settings)

    assertTrue(second is HashResult.Success)
  }

  @Test
  fun hash_fileOver50Mb_setsQuickSampleHash() {
    val chunk = ByteArray(HashConstants.SAMPLE_CHUNK_BYTES) { 0x5A }
    val payload = chunk + chunk
    val size = HashConstants.SAMPLE_SIZE_THRESHOLD_BYTES + 1
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(payload, logicalSizeBytes = size),
            sizeBucketIndex = object : SizeBucketIndex {
              override fun register(
                  sizeBytes: Long,
                  mediaTypeHint: MediaTypeHint,
                  isEmpty: Boolean,
              ): SizeBucketDisposition = SizeBucketDisposition.NEEDS_HASH
            },
        )

    val result = pipeline.hash(staged(sizeBytes = size))

    val success = result as HashResult.Success
    assertNotNull(success.hashed.quickSampleHash)
    assertEquals(64, success.hashed.quickSampleHash!!.length)
  }

  @Test
  fun hash_symlink_returnsSymlinkNodeWithoutRead() {
    val pipeline =
        HashPipeline(
            context,
            object : FileContentReader {
              override fun openRead(uri: Uri): ContentOpenOutcome {
                throw AssertionError("must not open symlink for read")
              }

              override fun readRange(uri: Uri, offset: Long, length: Int): ByteArray? {
                throw AssertionError("must not read symlink range")
              }
            },
        )
    val result = pipeline.hash(staged(sizeBytes = 10, isSymlink = true))

    assertTrue(result is HashResult.SymlinkNode)
  }

  private fun alwaysNeedsHashIndex(): SizeBucketIndex =
      object : SizeBucketIndex {
        override fun register(
            sizeBytes: Long,
            mediaTypeHint: MediaTypeHint,
            isEmpty: Boolean,
        ): SizeBucketDisposition = SizeBucketDisposition.NEEDS_HASH
      }

  private fun staged(
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint = MediaTypeHint.OTHER,
      isSymlink: Boolean = false,
  ): StagedFile {
    val entry =
        DiscoveredEntry(
            contentUri = contentUri,
            scanRootId = 1,
            generation = 1,
            displayName = "payload.bin",
            mediaTypeHint = mediaTypeHint,
            sizeBytes = sizeBytes,
            mtimeNs = 0,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = 0,
        inode = 1,
        deviceId = 1,
        isSymlink = isSymlink,
        mediaTypeHint = mediaTypeHint,
    )
  }

  private class FakeContentReader(
      private val payload: ByteArray,
      private val logicalSizeBytes: Long = payload.size.toLong(),
  ) : FileContentReader {

    override fun openRead(uri: Uri): ContentOpenOutcome =
        ContentOpenOutcome.Ok(ByteArrayInputStream(payload))

    override fun readRange(uri: Uri, offset: Long, length: Int): ByteArray? {
      if (offset >= logicalSizeBytes) {
        return ByteArray(0)
      }
      val end = minOf(offset + length, logicalSizeBytes)
      val sliceLen = (end - offset).toInt()
      val start = offset.toInt().coerceAtMost(payload.size)
      val available = (payload.size - start).coerceAtLeast(0)
      val copyLen = minOf(sliceLen, available)
      if (copyLen <= 0) {
        return ByteArray(sliceLen)
      }
      return payload.copyOfRange(start, start + copyLen)
        .let { read ->
          if (read.size == sliceLen) {
            read
          } else {
            ByteArray(sliceLen).also { out -> read.copyInto(out) }
          }
        }
    }
  }
}
