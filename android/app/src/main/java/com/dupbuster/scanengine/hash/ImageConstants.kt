package com.dupbuster.scanengine.hash

/** IMAGE_CONTENT_V1 caps (requirements FR-FP-10). */
object ImageConstants {
  const val FINGERPRINT_TIMEOUT_MS: Long = 30_000
  const val FINGERPRINT_BUDGET_BYTES: Long = 50L * 1024 * 1024
  const val FINGERPRINT_BUDGET_LARGE_OPT_IN_BYTES: Long = 2L * 1024 * 1024 * 1024
  /**
   * Slightly above video default (8): recompressed/resized exports (e.g. Downloads thumbnails)
   * often exceed 8 bits on a single dHash while still being the same picture (FR-FP-09 QA budget ≤ 10).
   */
  const val HAMMING_THRESHOLD_DEFAULT: Int = 10
}
