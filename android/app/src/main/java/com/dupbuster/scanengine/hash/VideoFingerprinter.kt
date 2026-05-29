package com.dupbuster.scanengine.hash

import android.net.Uri
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile

/**
 * Computes VIDEO_CONTENT_V1 (5-frame dHash + caps) for video files (FR-FP-07 / FR-FP-08).
 * Platform decode via injectable [VideoFrameExtractor]; production uses MediaCodec path in a later wiring task.
 */
class VideoFingerprinter(
    private val frameExtractor: VideoFrameExtractor,
) {

  sealed class Outcome {
    data class Success(val fingerprint: VideoFingerprint) : Outcome()

    data class Unscannable(val reason: String) : Outcome()
  }

  fun fingerprint(
      staged: StagedFile,
      settings: HashSettings = HashSettings(),
      startedAtMs: Long = System.currentTimeMillis(),
  ): Outcome {
    val uri = staged.discovered.contentUri
    val budget =
        if (settings.largeFilesOptIn) {
          VideoConstants.FINGERPRINT_BUDGET_LARGE_OPT_IN_BYTES
        } else {
          VideoConstants.FINGERPRINT_BUDGET_BYTES
        }
    if (staged.sizeBytes > budget) {
      return Outcome.Unscannable(UnscannableReason.VIDEO_DECODE_FAILED)
    }

    val deadlineMs = startedAtMs + VideoConstants.FINGERPRINT_TIMEOUT_MS
    val samplePositions = samplePositionsForDuration(staged.durationMs)
    return when (
        val extracted =
            frameExtractor.extractFrames(
                uri = uri,
                samplePositions = samplePositions,
                deadlineMs = deadlineMs,
            )
    ) {
      VideoFrameExtractOutcome.DecodeFailed ->
          Outcome.Unscannable(UnscannableReason.VIDEO_DECODE_FAILED)
      VideoFrameExtractOutcome.Timeout ->
          Outcome.Unscannable(UnscannableReason.VIDEO_DECODE_FAILED)
      is VideoFrameExtractOutcome.Ok -> {
        if (System.currentTimeMillis() > deadlineMs) {
          return Outcome.Unscannable(UnscannableReason.VIDEO_DECODE_FAILED)
        }
        val frameHashes =
            extracted.frames.map { frame ->
              DHash.compute(frame.pixels, frame.width, frame.height)
            }.toLongArray()
        if (frameHashes.isEmpty()) {
          return Outcome.Unscannable(UnscannableReason.VIDEO_DECODE_FAILED)
        }
        Outcome.Success(
            VideoFingerprint(
                frameHashes = frameHashes,
                durationMs = extracted.durationMs,
                videoWidth = extracted.videoWidth,
                videoHeight = extracted.videoHeight,
            ),
        )
      }
    }
  }

  companion object {
    fun samplePositionsForDuration(durationMs: Long): DoubleArray =
        if (durationMs in 1 until VideoConstants.MIN_DURATION_MULTI_FRAME_MS) {
          VideoConstants.SINGLE_FRAME_SAMPLE_POSITIONS
        } else {
          VideoConstants.FRAME_SAMPLE_POSITIONS
        }
  }
}
