package com.dupbuster.scanengine.security

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanTelemetryEgressTest {

  @After
  fun tearDown() {
    ScanTelemetryEgress.setCrashAnalyticsOptIn(false)
  }

  @Test
  fun default_optIn_isFalse() {
    assertFalse(ScanTelemetryEgress.isEgressEnabled())
  }

  @Test
  fun recordCrashPayload_noOpsWhenOptInDisabled() {
    val recorded =
        ScanTelemetryEgress.recordCrashPayload(
            mapOf(
                "scan_run_id" to 1L,
                "phase" to "hashing",
                "schema_version" to 2,
            ),
        )
    assertFalse(recorded)
  }

  @Test
  fun recordCrashPayload_stripsBannedKeysWhenOptInEnabled() {
    ScanTelemetryEgress.setCrashAnalyticsOptIn(true)
    val recorded =
        ScanTelemetryEgress.recordCrashPayload(
            mapOf(
                "scan_run_id" to 1L,
                "phase" to "hashing",
                "unscannable_reason" to "PERMISSION_DENIED",
                "uri_or_path" to "/storage/emulated/0/secret.jpg",
            ),
        )
    assertTrue(recorded)
    assertFalse(
        ScanTelemetryEgress.lastRecordedPayloadForTests().containsKey("unscannable_reason"),
    )
    assertFalse(
        ScanTelemetryEgress.lastRecordedPayloadForTests().containsKey("uri_or_path"),
    )
  }

  @Test
  fun recordCrashPayload_returnsFalseWhenOnlyBannedKeys() {
    ScanTelemetryEgress.setCrashAnalyticsOptIn(true)
    val recorded =
        ScanTelemetryEgress.recordCrashPayload(
            mapOf("unscannable_reason" to "HASH_TIMEOUT"),
        )
    assertFalse(recorded)
    assertTrue(ScanTelemetryEgress.lastRecordedPayloadForTests().isEmpty())
  }

  @Test
  fun sanitizeStillRedactsWhenOptInDisabled() {
    val sanitized =
        ScanTelemetryEgress.sanitizeCrashMessage("failed at /storage/emulated/0/a.jpg")!!
    assertEquals(RedactionFilter.REDACTED_PLACEHOLDER, sanitized.substringAfter("failed at "))
  }
}
