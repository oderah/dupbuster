package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.security.UnscannableReason

/**
 * Detects grant-level permission loss mid-scan (FR-SI-02 / AC-integrity-perm-01).
 * A root that previously succeeded and later returns [UnscannableReason.PERMISSION_DENIED]
 * is treated as revocation, not a single bad file.
 */
class GrantRevocationTracker {
  private val successfulScanRootIds = mutableSetOf<Long>()

  fun markSuccessfulAccess(scanRootId: Long) {
    successfulScanRootIds.add(scanRootId)
  }

  fun isGrantRevocation(scanRootId: Long, unscannableReason: String): Boolean =
      unscannableReason == UnscannableReason.PERMISSION_DENIED &&
          successfulScanRootIds.contains(scanRootId)

  fun reset() {
    successfulScanRootIds.clear()
  }
}
