package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.foreground.ScanCancelTeardownConstants
import java.util.concurrent.Executor

/**
 * Polls until [ScanCancelTeardownConstants.DRAIN_TIMEOUT_MS] after cancel, then invokes
 * [onDrainExpired] when the scan session is still in a cancelling state.
 */
class ScanCancelDrainWatchdog(
    private val drainTimeoutMs: Long = ScanCancelTeardownConstants.DRAIN_TIMEOUT_MS,
    private val pollIntervalMs: Long = 50L,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) {
  fun scheduleDrainTimeout(
      cancelStartedAtMs: Long,
      isStillCancelling: () -> Boolean,
      onDrainExpired: () -> Unit,
      executor: Executor,
  ) {
    executor.execute {
      val deadlineMs = cancelStartedAtMs + drainTimeoutMs
      while (clock() < deadlineMs) {
        if (!isStillCancelling()) {
          return@execute
        }
        sleeper(pollIntervalMs)
      }
      if (isStillCancelling()) {
        onDrainExpired()
      }
    }
  }
}
