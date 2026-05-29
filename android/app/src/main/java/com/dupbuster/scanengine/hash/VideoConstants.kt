package com.dupbuster.scanengine.hash

/** VIDEO_CONTENT_V1 caps (architecture §4.2 / FR-FP-08). */
object VideoConstants {
  const val FINGERPRINT_TIMEOUT_MS: Long = 90_000
  const val FINGERPRINT_BUDGET_BYTES: Long = 500L * 1024 * 1024
  const val FINGERPRINT_BUDGET_LARGE_OPT_IN_BYTES: Long = 2L * 1024 * 1024 * 1024
  const val HEADER_PARSE_CAP_BYTES: Long = 32L * 1024 * 1024
  const val MIN_DURATION_MULTI_FRAME_MS: Long = 3_000
  const val DHASH_MAX_WIDTH: Int = 320
  const val DHASH_MAX_HEIGHT: Int = 180
  const val DHASH_COMPARE_WIDTH: Int = 9
  const val DHASH_COMPARE_HEIGHT: Int = 8
  const val HAMMING_THRESHOLD_DEFAULT: Int = 8
  const val MIN_MATCHING_FRAME_PAIRS: Int = 3
  const val DURATION_GATE_RATIO: Double = 0.02
  const val DURATION_GATE_MIN_MS: Long = 1_000
  val FRAME_SAMPLE_POSITIONS: DoubleArray =
      doubleArrayOf(0.02, 0.25, 0.50, 0.75, 0.98)
  val SINGLE_FRAME_SAMPLE_POSITIONS: DoubleArray = doubleArrayOf(0.50)
}
