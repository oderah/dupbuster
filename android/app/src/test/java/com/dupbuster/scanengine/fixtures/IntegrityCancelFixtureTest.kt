package com.dupbuster.scanengine.fixtures

import com.dupbuster.scanengine.foreground.ScanCancelTeardownConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Descriptor-backed expectations for integrity-cancel-01 (AC-integrity-cancel-01). */
class IntegrityCancelFixtureTest {

  @Test
  fun integrityCancel01_matchesArchitectureTeardownBounds() {
    val fixture = FixtureLoader.load("integrity-cancel-01.json")
    assertEquals("integrity-cancel-01", fixture.getString("id"))
    assertEquals("android", fixture.getString("platform"))

    val input = fixture.getJSONObject("input")
    assertEquals(
        ScanCancelTeardownConstants.DRAIN_TIMEOUT_MS,
        input.getLong("drainTimeoutMs"),
    )
    assertEquals(
        ScanCancelTeardownConstants.NOTIFICATION_HARD_BOUND_MS,
        input.getLong("notificationHardBoundMs"),
    )
    assertEquals(
        ScanCancelTeardownConstants.PER_FILE_READ_CANCEL_CAP_MS,
        input.getLong("perFileReadCancelCapMs"),
    )

    val expect = fixture.getJSONObject("expect")
    assertTrue(expect.getBoolean("statusCancellingThenCancelled"))
    assertTrue(expect.getBoolean("openFdsClosedOnCancel"))
    assertTrue(expect.getBoolean("foregroundStoppedOnCancelled"))
    assertTrue(expect.getBoolean("notificationClearedWithinHardBound"))
    assertTrue(expect.getBoolean("noOrphanForegroundService"))
  }
}
