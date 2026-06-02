package com.dupbuster.scanengine.hash

import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile

/** Computes IMAGE_CONTENT_V1 (dHash on decoded bitmap) for image files (FR-FP-09 / FR-FP-10). */
class ImageFingerprinter(
    private val bitmapExtractor: ImageBitmapExtractor,
) {

  sealed class Outcome {
    data class Success(val fingerprint: ImageFingerprint) : Outcome()

    data class Unscannable(val reason: String) : Outcome()
  }

  fun fingerprint(
      staged: StagedFile,
      settings: HashSettings = HashSettings(),
      startedAtMs: Long = System.currentTimeMillis(),
  ): Outcome {
    val budget =
        if (settings.largeFilesOptIn) {
          ImageConstants.FINGERPRINT_BUDGET_LARGE_OPT_IN_BYTES
        } else {
          ImageConstants.FINGERPRINT_BUDGET_BYTES
        }
    if (staged.sizeBytes > budget) {
      return Outcome.Unscannable(UnscannableReason.IMAGE_DECODE_FAILED)
    }

    val deadlineMs = startedAtMs + ImageConstants.FINGERPRINT_TIMEOUT_MS
    return when (val decoded = bitmapExtractor.decode(staged.discovered.contentUri, deadlineMs)) {
      ImageBitmapExtractor.Outcome.DecodeFailed ->
          Outcome.Unscannable(UnscannableReason.IMAGE_DECODE_FAILED)
      is ImageBitmapExtractor.Outcome.Ok -> {
        if (System.currentTimeMillis() > deadlineMs) {
          return Outcome.Unscannable(UnscannableReason.IMAGE_DECODE_FAILED)
        }
        val dHash = DHash.compute(decoded.frame.pixels, decoded.frame.width, decoded.frame.height)
        Outcome.Success(ImageFingerprint(dHash = dHash))
      }
    }
  }
}
