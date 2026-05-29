package com.dupbuster.scanengine.hash

import android.net.Uri
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoFingerprinterTest {

  private val contentUri = Uri.parse("content://test/video/clip.mp4")

  @Test
  fun fingerprint_shortClip_usesSingleFrameSample() {
    val positions = mutableListOf<DoubleArray>()
    val fingerprinter =
        VideoFingerprinter(
            object : VideoFrameExtractor {
              override fun extractFrames(
                  uri: Uri,
                  samplePositions: DoubleArray,
                  deadlineMs: Long,
              ): VideoFrameExtractOutcome {
                positions.add(samplePositions.copyOf())
                return VideoFrameExtractOutcome.Ok(
                    frames = listOf(grayFrame(8, 8, 1)),
                    durationMs = 2_000,
                    videoWidth = 640,
                    videoHeight = 360,
                )
              }
            },
        )

    val result = fingerprinter.fingerprint(staged(durationMs = 2_000))

    assertTrue(result is VideoFingerprinter.Outcome.Success)
    assertEquals(1, positions.single().size)
    assertEquals(0.50, positions.single()[0], 0.001)
  }

  @Test
  fun fingerprint_longClip_samplesFivePositions() {
    val fingerprinter =
        VideoFingerprinter(
            object : VideoFrameExtractor {
              override fun extractFrames(
                  uri: Uri,
                  samplePositions: DoubleArray,
                  deadlineMs: Long,
              ): VideoFrameExtractOutcome {
                assertEquals(5, samplePositions.size)
                return VideoFrameExtractOutcome.Ok(
                    frames = List(5) { grayFrame(8, 8, it) },
                    durationMs = 10_000,
                    videoWidth = 1920,
                    videoHeight = 1080,
                )
              }
            },
        )

    val result = fingerprinter.fingerprint(staged(durationMs = 10_000))

    val success = result as VideoFingerprinter.Outcome.Success
    assertEquals(5, success.fingerprint.frameHashes.size)
    assertEquals(64, success.fingerprint.hashValue.length)
  }

  @Test
  fun fingerprint_exceedsWallClockBudget_returnsVideoDecodeFailed_acSecurityDecode02() {
    val fingerprinter = VideoFingerprinter(FakeFrameExtractor())
    val startedAt =
        System.currentTimeMillis() - VideoConstants.FINGERPRINT_TIMEOUT_MS - 1_000

    val result = fingerprinter.fingerprint(staged(), startedAtMs = startedAt)

    val unscannable = result as VideoFingerprinter.Outcome.Unscannable
    assertEquals(UnscannableReason.VIDEO_DECODE_FAILED, unscannable.reason)
  }

  @Test
  fun fingerprint_overBudgetWithoutOptIn_returnsVideoDecodeFailed() {
    val fingerprinter = VideoFingerprinter(FakeFrameExtractor())
    val overBudget = VideoConstants.FINGERPRINT_BUDGET_BYTES + 1

    val result = fingerprinter.fingerprint(staged(sizeBytes = overBudget))

    val unscannable = result as VideoFingerprinter.Outcome.Unscannable
    assertEquals(UnscannableReason.VIDEO_DECODE_FAILED, unscannable.reason)
  }

  @Test
  fun samplePositionsForDuration_switchesAtThreeSeconds() {
    assertEquals(1, VideoFingerprinter.samplePositionsForDuration(2_999).size)
    assertEquals(5, VideoFingerprinter.samplePositionsForDuration(3_000).size)
  }

  private fun staged(
      durationMs: Long = 10_000,
      sizeBytes: Long = 1024,
  ): StagedFile {
    val entry =
        DiscoveredEntry(
            contentUri = contentUri,
            scanRootId = 1,
            generation = 1,
            displayName = "clip.mp4",
            mediaTypeHint = MediaTypeHint.VIDEO,
            sizeBytes = sizeBytes,
            mtimeNs = 0,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = sizeBytes,
        mtimeNs = 0,
        inode = 1,
        deviceId = 1,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.VIDEO,
        durationMs = durationMs,
        videoWidth = 640,
        videoHeight = 360,
    )
  }

  private fun grayFrame(width: Int, height: Int, seed: Int): GrayFrame {
    val pixels = ByteArray(width * height) { index -> ((index + seed) % 256).toByte() }
    return GrayFrame(width, height, pixels)
  }

  private class FakeFrameExtractor : VideoFrameExtractor {
    override fun extractFrames(
        uri: Uri,
        samplePositions: DoubleArray,
        deadlineMs: Long,
    ): VideoFrameExtractOutcome =
        VideoFrameExtractOutcome.Ok(
            frames = listOf(GrayFrame(8, 8, ByteArray(64) { 0x5A })),
            durationMs = 5_000,
            videoWidth = 640,
            videoHeight = 360,
        )
  }
}
