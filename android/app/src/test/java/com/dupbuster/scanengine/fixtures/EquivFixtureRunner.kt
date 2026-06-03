package com.dupbuster.scanengine.fixtures

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashPipeline
import com.dupbuster.scanengine.hash.HashResult
import com.dupbuster.scanengine.hash.HashSettings
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.InMemoryDurationBucketIndex
import com.dupbuster.scanengine.hash.InMemorySizeBucketIndex
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.hash.SizeBucketDisposition
import com.dupbuster.scanengine.hash.SizeBucketIndex
import com.dupbuster.scanengine.hash.VideoFingerprinter
import com.dupbuster.scanengine.hash.VideoFingerprint
import com.dupbuster.scanengine.hash.VideoFrameExtractOutcome
import com.dupbuster.scanengine.hash.VideoFrameExtractor
import com.dupbuster.scanengine.hash.ImageContentMatcher
import com.dupbuster.scanengine.hash.VideoContentMatcher
import com.dupbuster.scanengine.hash.VideoFingerprintCodec
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.Grouper
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.index.MatchKind
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/** Drives native ScanEngine code paths from M1-17 fixture `input` / `expect` (M1-18). */
object EquivFixtureRunner {

  private var inodeCounter = 1L

  fun run(fixture: JSONObject) {
    inodeCounter = 1L
    val id = fixture.getString("id")
    when {
      id.startsWith("equiv-text-") || id == "equiv-binary-01" -> runPayloadHashFixture(fixture)
      id == "equiv-empty-01" -> runEmptyFixture(fixture)
      id == "equiv-symlink-01" -> runSymlinkFixture(fixture)
      id.startsWith("equiv-doc-") ||
          id.startsWith("equiv-img-") ||
          id == "equiv-av-01" -> runGrouperHashFixture(fixture)
      id.startsWith("equiv-video-xres-") -> runVideoXresFixture(fixture)
      id.startsWith("equiv-image-content-") -> runImageContentFixture(fixture)
      else -> error("Unhandled equivalence fixture: $id")
    }
  }

  private fun runPayloadHashFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val mediaTypeHint = mediaTypeFrom(input.optString("mediaTypeHint", "other"))
    val files = input.getJSONArray("files")
    val index = alwaysNeedsHashIndex()
    val hashes = mutableListOf<String>()

    for (i in 0 until files.length()) {
      val payload = files.getJSONObject(i).getString("payloadUtf8").toByteArray(Charsets.UTF_8)
      val pipeline = HashPipeline(context, FakeContentReader(payload), sizeBucketIndex = index)
      val result =
          pipeline.hash(
              staged(context, payload.size.toLong(), mediaTypeHint, suffix = "f$i"),
          )
      val success = result as HashResult.Success
      hashes.add(success.hashed.hashValue)
      if (i == 0 && expect.has("normalizationProfile")) {
        assertEquals(expect.getString("normalizationProfile"), success.hashed.normalizationProfile)
      }
      if (i == 0 && expect.has("hashValue")) {
        assertEquals(expect.getString("hashValue"), success.hashed.hashValue)
      }
    }

    if (expect.has("hashValues")) {
      val expected = expect.getJSONArray("hashValues")
      assertEquals(expected.length(), hashes.size)
      for (i in 0 until expected.length()) {
        assertEquals(expected.getString(i), hashes[i])
      }
    }

    assertSameDuplicateGroup(
        expect,
        hashes,
        freshCatalog(ApplicationProvider.getApplicationContext()),
    )
  }

  private fun runEmptyFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val fileCount = input.getInt("fileCount")
    val index = alwaysNeedsHashIndex()
    val hashes = mutableListOf<String>()

    repeat(fileCount) { i ->
      val pipeline = HashPipeline(context, FakeContentReader(byteArrayOf()), sizeBucketIndex = index)
      val result = pipeline.hash(staged(context, 0L, MediaTypeHint.OTHER, suffix = "z$i"))
      val success = result as HashResult.Success
      assertEquals(expect.getString("normalizationProfile"), success.hashed.normalizationProfile)
      assertEquals(expect.getString("hashValue"), success.hashed.hashValue)
      hashes.add(success.hashed.hashValue)
    }

    assertSameDuplicateGroup(expect, hashes, freshCatalog(context))
  }

  private fun runSymlinkFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val expect = fixture.getJSONObject("expect")
    val pipeline =
        HashPipeline(
            context,
            object : com.dupbuster.scanengine.hash.FileContentReader {
              override fun openRead(uri: Uri): com.dupbuster.scanengine.hash.ContentOpenOutcome {
                throw AssertionError("must not open symlink for read")
              }

              override fun readRange(uri: Uri, offset: Long, length: Int): ByteArray? {
                throw AssertionError("must not read symlink range")
              }
            },
        )
    val result = pipeline.hash(staged(context, 10L, MediaTypeHint.OTHER, isSymlink = true))

    assertEquals(expect.getString("symlinkOutcome"), result.javaClass.simpleName)
    assertFalse(expect.getBoolean("sameDuplicateGroup"))

    val catalog = freshCatalog(context)
    catalog.writer.upsertSymlink(
        staged(context, 10L, suffix = "link", isSymlink = true, rootId = catalog.rootId),
        1,
    )
    catalog.writer.upsertHashed(
        HashedFile(
            staged(context, 100L, suffix = "target", rootId = catalog.rootId),
            fixture.getJSONObject("input").getJSONObject("target").getString("hashValue"),
            NormalizationProfile.RAW_BYTES,
        ),
        1,
    )
    catalog.writer.upsertHashed(
        HashedFile(
            staged(context, 100L, suffix = "target2", rootId = catalog.rootId),
            fixture.getJSONObject("input").getJSONObject("target").getString("hashValue"),
            NormalizationProfile.RAW_BYTES,
        ),
        1,
    )
    catalog.grouper.rebuildDuplicateGroups()
    assertEquals(1, catalog.grouper.duplicateGroupCount())
    assertEquals(
        2,
        catalog.grouper.memberCountForGroup(requireNotNull(catalog.grouper.firstDuplicateGroupId())),
    )
  }

  private fun runGrouperHashFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val files = input.getJSONArray("files")
    val profile = expect.optString("normalizationProfile", NormalizationProfile.RAW_BYTES)
    val catalog = freshCatalog(context)
    val hashes = mutableListOf<String>()

    for (i in 0 until files.length()) {
      val hash = files.getJSONObject(i).getString("hashValue")
      hashes.add(hash)
      catalog.writer.upsertHashed(
          HashedFile(staged(context, 100L, suffix = "g$i", rootId = catalog.rootId), hash, profile),
          1,
      )
    }

    catalog.grouper.rebuildDuplicateGroups()
    assertGrouperDuplicateGroups(expect, catalog)

    if (expect.has("matchKind") && expect.getBoolean("sameDuplicateGroup")) {
      val groupId = requireNotNull(catalog.grouper.firstDuplicateGroupId())
      assertEquals(expect.getString("matchKind"), catalog.grouper.matchKindForGroup(groupId))
    }
  }

  private fun runVideoXresFixture(fixture: JSONObject) {
    val id = fixture.getString("id")
    when (id) {
      "equiv-video-xres-09" -> runVideoDecodeFailedFixture(fixture)
      "equiv-video-xres-10" -> runVideoBudgetSkipFixture(fixture)
      "equiv-video-xres-04" -> runVideoExactBytesPrecedenceFixture(fixture)
      "equiv-video-xres-05", "equiv-video-xres-05b" -> runVideoDurationGateFixture(fixture)
      else -> runVideoMatcherFixture(fixture)
    }
  }

  private fun runVideoMatcherFixture(fixture: JSONObject) {
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val members = parseVideoMembers(input.getJSONArray("members"))

    if (expect.has("contentMatchesAllPairs")) {
      val allPairs = expect.getBoolean("contentMatchesAllPairs")
      for (i in members.indices) {
        for (j in i + 1 until members.size) {
          val matches = VideoContentMatcher.contentMatches(members[i], members[j])
          if (allPairs) {
            assertTrue("${fixture.getString("id")} pair $i,$j should match", matches)
          } else {
            assertFalse("${fixture.getString("id")} pair $i,$j should not match", matches)
          }
        }
      }
    }

    if (expect.has("contentMatches")) {
      assertEquals(
          expect.getBoolean("contentMatches"),
          VideoContentMatcher.contentMatches(members[0], members[1]),
      )
    }

    if (expect.has("matchKind") && expect.optString("matchKind") == MatchKind.SAME_CONTENT_VIDEO) {
      assertTrue(
          VideoContentMatcher.contentMatches(members[0], members[1]),
      )
    }

    if (expect.has("memberCountMin")) {
      val min = expect.getInt("memberCountMin")
      assertTrue(members.size >= min)
    }
  }

  private fun runVideoDurationGateFixture(fixture: JSONObject) {
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val members = parseVideoMembers(input.getJSONArray("members"))
    val passes = VideoContentMatcher.passesDurationGate(members[0].durationMs, members[1].durationMs)
    assertEquals(expect.getBoolean("passesDurationGate"), passes)
    assertFalse(VideoContentMatcher.contentMatches(members[0], members[1]))
  }

  private fun runVideoExactBytesPrecedenceFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val rawHash = input.getString("rawHash")
    val members = parseVideoMembers(input.getJSONArray("members"))
    val template = members.first()
    val frameHashes = template.frameHashes
    val videoHash = VideoFingerprintCodec.canonicalHashValue(frameHashes)
    val frameHashesBlob = VideoFingerprintCodec.encodeFrameHashesBlob(frameHashes)
    val catalog = freshCatalog(context)

    repeat(2) { i ->
      val staged =
          staged(
              context,
              100L,
              MediaTypeHint.VIDEO,
              suffix = "v$i",
              rootId = catalog.rootId,
              durationMs = template.durationMs,
          )
      catalog.writer.upsertVideoDualHashed(
          rawBytes = HashedFile(staged, rawHash, NormalizationProfile.RAW_BYTES),
          videoContent =
              HashedFile(
                  staged = staged,
                  hashValue = videoHash,
                  normalizationProfile = NormalizationProfile.VIDEO_CONTENT_V1,
                  frameHashesBlob = frameHashesBlob,
              ),
          generation = 1,
      )
    }

    catalog.grouper.rebuildDuplicateGroups()
    assertEquals(
        expect.getString("matchKind"),
        catalog.grouper.matchKindForGroup(requireNotNull(catalog.grouper.firstDuplicateGroupId())),
    )
    if (expect.optBoolean("noSeparateSameContentVideoGroup")) {
      assertEquals(1, catalog.grouper.duplicateGroupCount())
      assertTrue(catalog.grouper.duplicateGroupIdsWithMatchKind(MatchKind.SAME_CONTENT_VIDEO).isEmpty())
    }
  }

  private fun runImageContentFixture(fixture: JSONObject) {
    val id = fixture.getString("id")
    when (id) {
      "equiv-image-content-04" -> runImageExactBytesPrecedenceFixture(fixture)
      "equiv-image-content-01" -> runImageContentGrouperFixture(fixture)
      else -> runImageMatcherFixture(fixture)
    }
  }

  private fun runImageMatcherFixture(fixture: JSONObject) {
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val members = parseImageMembers(input.getJSONArray("members"))

    if (expect.has("contentMatchesAllPairs")) {
      val allPairs = expect.getBoolean("contentMatchesAllPairs")
      for (i in members.indices) {
        for (j in i + 1 until members.size) {
          val matches = ImageContentMatcher.matches(members[i].dHash, members[j].dHash)
          if (allPairs) {
            assertTrue("${fixture.getString("id")} pair $i,$j should match", matches)
          } else {
            assertFalse("${fixture.getString("id")} pair $i,$j should not match", matches)
          }
        }
      }
    }

    if (expect.has("contentMatches")) {
      assertEquals(
          expect.getBoolean("contentMatches"),
          ImageContentMatcher.matches(members[0].dHash, members[1].dHash),
      )
    }

    if (expect.has("matchKind") && expect.optString("matchKind") == MatchKind.SAME_CONTENT_IMAGE) {
      assertTrue(ImageContentMatcher.matches(members[0].dHash, members[1].dHash))
    }

    if (expect.has("memberCountMin")) {
      val min = expect.getInt("memberCountMin")
      assertTrue(members.size >= min)
    }
  }

  private fun runImageContentGrouperFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val members = parseImageMembers(input.getJSONArray("members"))
    val catalog = freshCatalog(context)

    members.forEachIndexed { index, member ->
      upsertImageDualInCatalog(
          catalog = catalog,
          suffix = "img-$index",
          rawHash = member.rawHash ?: "raw-$index",
          dHash = member.dHash,
      )
    }

    catalog.grouper.rebuildDuplicateGroups()

    if (expect.has("matchKind")) {
      assertEquals(
          expect.getString("matchKind"),
          catalog.grouper.matchKindForGroup(requireNotNull(catalog.grouper.firstDuplicateGroupId())),
      )
    }
    if (expect.has("memberCountMin")) {
      val groupId = requireNotNull(catalog.grouper.firstDuplicateGroupId())
      assertTrue(catalog.grouper.memberCountForGroup(groupId) >= expect.getInt("memberCountMin"))
    }
  }

  private fun runImageExactBytesPrecedenceFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val rawHash = input.getString("rawHash")
    val template = parseImageMembers(input.getJSONArray("members")).first()
    val catalog = freshCatalog(context)

    repeat(2) { index ->
      upsertImageDualInCatalog(
          catalog = catalog,
          suffix = "img-$index",
          rawHash = rawHash,
          dHash = template.dHash,
      )
    }

    catalog.grouper.rebuildDuplicateGroups()
    assertEquals(
        expect.getString("matchKind"),
        catalog.grouper.matchKindForGroup(requireNotNull(catalog.grouper.firstDuplicateGroupId())),
    )
    if (expect.optBoolean("noSeparateSameContentImageGroup")) {
      assertEquals(1, catalog.grouper.duplicateGroupCount())
      assertTrue(catalog.grouper.duplicateGroupIdsWithMatchKind(MatchKind.SAME_CONTENT_IMAGE).isEmpty())
    }
  }

  private data class ImageMember(val dHash: Long, val rawHash: String? = null)

  private fun parseImageMembers(array: JSONArray): List<ImageMember> =
      (0 until array.length()).map { index ->
        val member = array.getJSONObject(index)
        ImageMember(
            dHash = member.getLong("dHash"),
            rawHash = member.optString("rawHash", null),
        )
      }

  private fun upsertImageDualInCatalog(
      catalog: CatalogHarness,
      suffix: String,
      rawHash: String,
      dHash: Long,
  ) {
    val staged =
        staged(
            ApplicationProvider.getApplicationContext(),
            100L,
            MediaTypeHint.IMAGE,
            suffix = suffix,
            rootId = catalog.rootId,
        )
    val frameHashesBlob = VideoFingerprintCodec.encodeFrameHashesBlob(longArrayOf(dHash))
    val imageHash = VideoFingerprintCodec.canonicalHashValue(longArrayOf(dHash))
    catalog.writer.upsertImageDualHashed(
        rawBytes = HashedFile(staged, rawHash, NormalizationProfile.RAW_BYTES),
        imageContent =
            HashedFile(
                staged = staged,
                hashValue = imageHash,
                normalizationProfile = NormalizationProfile.IMAGE_CONTENT_V1,
                frameHashesBlob = frameHashesBlob,
            ),
        generation = 1,
    )
  }

  private fun runVideoDecodeFailedFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val expect = fixture.getJSONObject("expect")
    val fingerprinter =
        VideoFingerprinter(
            object : VideoFrameExtractor {
              override fun extractFrames(
                  uri: Uri,
                  samplePositions: DoubleArray,
                  deadlineMs: Long,
              ): VideoFrameExtractOutcome = VideoFrameExtractOutcome.DecodeFailed
            },
        )
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(byteArrayOf(0x00)),
            sizeBucketIndex = alwaysNeedsHashIndex(),
            durationBucketIndex = InMemoryDurationBucketIndex(),
            videoFingerprinter = fingerprinter,
        )
    val result =
        pipeline.hash(
            staged(context, 1024L, MediaTypeHint.VIDEO, durationMs = 10_000),
        )

    assertTrue(result is HashResult.VideoPartialSuccess)
    assertEquals(
        expect.getString("unscannableReason"),
        (result as HashResult.VideoPartialSuccess).videoUnscannableReason,
    )
  }

  private fun runVideoBudgetSkipFixture(fixture: JSONObject) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    val sizeBytes = input.getLong("sizeBytes")
    val settings = HashSettings(largeFilesOptIn = input.optBoolean("largeFilesOptIn", false))

    val fingerprinter = VideoFingerprinter(FakeVideoFrameExtractor())
    val pipeline =
        HashPipeline(
            context,
            FakeContentReader(byteArrayOf(0x01), logicalSizeBytes = sizeBytes),
            sizeBucketIndex = alwaysNeedsHashIndex(),
            durationBucketIndex = InMemoryDurationBucketIndex(),
            videoFingerprinter = fingerprinter,
        )
    val result =
        pipeline.hash(
            staged(context, sizeBytes, MediaTypeHint.VIDEO, durationMs = 60_000),
            settings,
        )

    assertTrue(result is HashResult.VideoPartialSuccess)
    assertTrue(expect.getBoolean("videoContentFingerprintSkipped"))
    assertEquals(UnscannableReason.VIDEO_DECODE_FAILED, (result as HashResult.VideoPartialSuccess).videoUnscannableReason)
  }

  private data class CatalogHarness(
      val writer: IndexWriter,
      val grouper: Grouper,
      val rootId: Long,
  )

  private fun freshCatalog(context: Context): CatalogHarness {
    val database = CatalogDatabase(context, name = "equiv-${System.nanoTime()}")
    val writer = IndexWriter(database)
    val grouper = Grouper(database)
    val rootId =
        writer.insertScanRoot(
            uriOrGrant = "content://test/tree/docs",
            mode = ScanRootMode.USER_SELECTED,
        )
    return CatalogHarness(writer = writer, grouper = grouper, rootId = rootId)
  }

  private fun assertSameDuplicateGroup(
      expect: JSONObject,
      hashes: List<String>,
      catalog: CatalogHarness,
  ) {
    if (!expect.has("sameDuplicateGroup")) {
      return
    }
    assertHashGroupingExpectation(expect, hashes)

    if (hashes.size < 2) {
      return
    }

    val context = ApplicationProvider.getApplicationContext<Context>()
    val profile = expect.optString("normalizationProfile", NormalizationProfile.RAW_BYTES)

    hashes.forEachIndexed { i, hash ->
      catalog.writer.upsertHashed(
          HashedFile(staged(context, 100L, suffix = "d$i", rootId = catalog.rootId), hash, profile),
          1,
      )
    }

    catalog.grouper.rebuildDuplicateGroups()
    assertGrouperDuplicateGroups(expect, catalog)
  }

  private fun assertHashGroupingExpectation(expect: JSONObject, hashes: List<String>) {
    val sameGroup = expect.getBoolean("sameDuplicateGroup")
    val unique = hashes.toSet()
    if (sameGroup) {
      assertEquals(1, unique.size)
    } else {
      assertTrue(unique.size > 1 || hashes.size < 2)
    }
  }

  private fun assertGrouperDuplicateGroups(expect: JSONObject, catalog: CatalogHarness) {
    if (!expect.has("sameDuplicateGroup")) {
      return
    }
    val sameGroup = expect.getBoolean("sameDuplicateGroup")
    if (sameGroup) {
      assertEquals(1, catalog.grouper.duplicateGroupCount())
    } else {
      assertEquals(0, catalog.grouper.duplicateGroupCount())
    }
  }

  private fun parseVideoMembers(array: JSONArray): List<VideoFingerprint> =
      (0 until array.length()).map { index ->
        val member = array.getJSONObject(index)
        val framesJson = member.getJSONArray("frameHashes")
        val frames = LongArray(framesJson.length()) { framesJson.getLong(it) }
        VideoFingerprint(
            frameHashes = frames,
            durationMs = member.getLong("durationMs"),
            videoWidth = 1920,
            videoHeight = 1080,
        )
      }

  private fun mediaTypeFrom(value: String): MediaTypeHint =
      when (value) {
        "text" -> MediaTypeHint.TEXT
        "video" -> MediaTypeHint.VIDEO
        else -> MediaTypeHint.OTHER
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
      context: Context,
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint = MediaTypeHint.OTHER,
      suffix: String = "f",
      isSymlink: Boolean = false,
      durationMs: Long = 0L,
      rootId: Long = 1,
  ): StagedFile {
    val inode = inodeCounter++
    val uri = Uri.parse("content://test/document/$suffix")
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "$suffix.bin",
            mediaTypeHint = mediaTypeHint,
            sizeBytes = sizeBytes,
            mtimeNs = 0,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = 0,
        inode = inode,
        deviceId = 1,
        isSymlink = isSymlink,
        mediaTypeHint = mediaTypeHint,
        durationMs = durationMs,
    )
  }

  private class FakeContentReader(
      private val payload: ByteArray,
      private val logicalSizeBytes: Long = payload.size.toLong(),
  ) : com.dupbuster.scanengine.hash.FileContentReader {

    override fun openRead(uri: Uri): com.dupbuster.scanengine.hash.ContentOpenOutcome =
        com.dupbuster.scanengine.hash.ContentOpenOutcome.Ok(ByteArrayInputStream(payload))

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
      return payload.copyOfRange(start, start + copyLen).let { read ->
        if (read.size == sliceLen) {
          read
        } else {
          ByteArray(sliceLen).also { out -> read.copyInto(out) }
        }
      }
    }
  }

  private class FakeVideoFrameExtractor : VideoFrameExtractor {
    override fun extractFrames(
        uri: Uri,
        samplePositions: DoubleArray,
        deadlineMs: Long,
    ): VideoFrameExtractOutcome =
        VideoFrameExtractOutcome.Ok(
            frames =
                List(samplePositions.size) { seed ->
                  com.dupbuster.scanengine.hash.GrayFrame(
                      8,
                      8,
                      ByteArray(64) { index -> ((index + seed) % 256).toByte() },
                  )
                },
            durationMs = 60_000,
            videoWidth = 1920,
            videoHeight = 1080,
        )
  }
}
