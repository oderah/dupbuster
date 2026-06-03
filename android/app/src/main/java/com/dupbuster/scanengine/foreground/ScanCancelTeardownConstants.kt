package com.dupbuster.scanengine.foreground

/** Architecture §7.1 FGS cancel teardown + FR-SI-05 / AC-integrity-cancel-01 bounds. */
object ScanCancelTeardownConstants {
  /** Per-file read abort cap after user cancel (architecture step 2). */
  const val PER_FILE_READ_CANCEL_CAP_MS: Long = 5_000L

  /** Executor drain bound before FGS stop (architecture step 3). */
  const val DRAIN_TIMEOUT_MS: Long = 30_000L

  /** Notification clear hard bound from cancel request (FR-SI-05). */
  const val NOTIFICATION_HARD_BOUND_MS: Long = 120_000L
}
