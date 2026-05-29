package com.dupbuster.scanengine.hash

/** Fingerprint pipeline thresholds (architecture §4.1–4.2 / FR-FP-01–04). */
object HashConstants {
  const val SAMPLE_SIZE_THRESHOLD_BYTES: Long = 50L * 1024 * 1024
  const val SAMPLE_CHUNK_BYTES: Int = 64 * 1024
  const val HASH_READ_BUFFER_BYTES: Int = 1024 * 1024
  const val LARGE_FILE_CAP_BYTES: Long = 2L * 1024 * 1024 * 1024
  const val HASH_TIMEOUT_MS: Long = 120_000
}
