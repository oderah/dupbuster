package com.dupbuster.scanengine.hash

import android.net.Uri

/** Extracts grayscale frames at normalized timeline positions for [VideoFingerprinter]. */
interface VideoFrameExtractor {
  fun extractFrames(
      uri: Uri,
      samplePositions: DoubleArray,
      deadlineMs: Long,
  ): VideoFrameExtractOutcome
}

sealed class VideoFrameExtractOutcome {
  data class Ok(
      val frames: List<GrayFrame>,
      val durationMs: Long,
      val videoWidth: Int,
      val videoHeight: Int,
  ) : VideoFrameExtractOutcome()

  data object DecodeFailed : VideoFrameExtractOutcome()

  data object Timeout : VideoFrameExtractOutcome()
}
