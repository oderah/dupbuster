package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.security.UnscannableReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrantRevocationTrackerTest {

  private val tracker = GrantRevocationTracker()

  @Test
  fun isGrantRevocation_falseBeforeSuccessfulAccess() {
    assertFalse(
        tracker.isGrantRevocation(
            scanRootId = 1L,
            unscannableReason = UnscannableReason.PERMISSION_DENIED,
        ),
    )
  }

  @Test
  fun isGrantRevocation_trueAfterSuccessfulAccessOnSameRoot() {
    tracker.markSuccessfulAccess(1L)
    assertTrue(
        tracker.isGrantRevocation(
            scanRootId = 1L,
            unscannableReason = UnscannableReason.PERMISSION_DENIED,
        ),
    )
  }

  @Test
  fun isGrantRevocation_falseForOtherReasons() {
    tracker.markSuccessfulAccess(1L)
    assertFalse(
        tracker.isGrantRevocation(
            scanRootId = 1L,
            unscannableReason = UnscannableReason.HASH_TIMEOUT,
        ),
    )
  }

  @Test
  fun reset_clearsSuccessfulRoots() {
    tracker.markSuccessfulAccess(1L)
    tracker.reset()
    assertFalse(
        tracker.isGrantRevocation(
            scanRootId = 1L,
            unscannableReason = UnscannableReason.PERMISSION_DENIED,
        ),
    )
  }
}
