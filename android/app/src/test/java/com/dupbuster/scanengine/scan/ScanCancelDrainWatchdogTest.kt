package com.dupbuster.scanengine.scan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScanCancelDrainWatchdogTest {

  @Test
  fun drainTimeout_invokesCallbackWhenStillCancelling() {
    val stillCancelling = AtomicBoolean(true)
    var expired = false
    val clock = object {
      var now = 0L
      fun get(): Long = now
    }
    val watchdog =
        ScanCancelDrainWatchdog(
            drainTimeoutMs = 100L,
            pollIntervalMs = 10L,
            clock = clock::get,
            sleeper = { ms -> clock.now += ms },
        )

    watchdog.scheduleDrainTimeout(
        cancelStartedAtMs = 0L,
        isStillCancelling = stillCancelling::get,
        onDrainExpired = { expired = true },
        executor = Executors.newSingleThreadExecutor(),
    )

    Thread.sleep(300)
    assertTrue(expired)
  }

  @Test
  fun drainTimeout_skipsCallbackWhenCancelledEarly() {
    val stillCancelling = AtomicBoolean(true)
    var expired = false
    val clock = object {
      var now = 0L
      fun get(): Long = now
    }
    val watchdog =
        ScanCancelDrainWatchdog(
            drainTimeoutMs = 100L,
            pollIntervalMs = 10L,
            clock = clock::get,
            sleeper = { ms ->
              if (clock.now >= 20L) {
                stillCancelling.set(false)
              }
              clock.now += ms
            },
        )

    watchdog.scheduleDrainTimeout(
        cancelStartedAtMs = 0L,
        isStillCancelling = stillCancelling::get,
        onDrainExpired = { expired = true },
        executor = Executors.newSingleThreadExecutor(),
    )

    Thread.sleep(300)
    assertFalse(expired)
  }
}
