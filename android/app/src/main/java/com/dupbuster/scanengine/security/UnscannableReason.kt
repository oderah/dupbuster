package com.dupbuster.scanengine.security

/** Closed unscannable reason codes (architecture §5 / FR-UN-02). */
object UnscannableReason {
  const val PERMISSION_DENIED: String = "PERMISSION_DENIED"
  const val LARGE_SKIPPED: String = "LARGE_SKIPPED"
  const val HASH_TIMEOUT: String = "HASH_TIMEOUT"
}
