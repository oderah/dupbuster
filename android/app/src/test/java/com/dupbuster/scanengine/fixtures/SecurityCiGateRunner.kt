package com.dupbuster.scanengine.fixtures

import com.dupbuster.scanengine.security.RedactionFilter
import com.dupbuster.scanengine.security.ScanTelemetryEgress
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/** Runs M4 CI security gates from fixture `expect.ciGate` descriptors (AC-security-redact-01). */
object SecurityCiGateRunner {

  fun run(fixture: JSONObject) {
    when (fixture.getString("id")) {
      "security-redact-01" -> runSecurityRedact01(fixture)
      else -> error("Unhandled ciGate fixture: ${fixture.getString("id")}")
    }
  }

  private fun runSecurityRedact01(fixture: JSONObject) {
    val rawPath = fixture.getJSONObject("input").getString("rawPath")
    val expect = fixture.getJSONObject("expect")
    require(expect.getBoolean("ciGate")) { "fixture must set expect.ciGate" }
    require(expect.getBoolean("mustNotContainRawPath")) {
      "fixture must set expect.mustNotContainRawPath"
    }

    val redacted = RedactionFilter.apply(rawPath)!!
    assertTextMustNotContainRawPath(redacted, rawPath)
    assertTrue(redacted.contains(RedactionFilter.REDACTED_PLACEHOLDER))

    val crashMessage =
        ScanTelemetryEgress.sanitizeCrashMessage("open failed at $rawPath")!!
    assertTextMustNotContainRawPath(crashMessage, rawPath)

    val crashPayload =
        mapOf(
            "scan_run_id" to 1L,
            "uri_or_path" to rawPath,
            "display_name" to "test.jpg",
            "detail" to "failed at $rawPath",
        )
    val sanitizedPayload = ScanTelemetryEgress.sanitizeCrashPayload(crashPayload)
    assertCrashPayloadMustNotContainRawPath(sanitizedPayload, rawPath)
    assertEquals(RedactionFilter.REDACTED_PLACEHOLDER, sanitizedPayload["uri_or_path"])
    assertEquals(RedactionFilter.REDACTED_PLACEHOLDER, sanitizedPayload["display_name"])

    val exceptionLine =
        ScanTelemetryEgress.sanitizeExceptionForTelemetry(RuntimeException("failed: $rawPath"))
    assertTextMustNotContainRawPath(exceptionLine, rawPath)
  }

  private fun assertTextMustNotContainRawPath(text: String, rawPath: String) {
    assertFalse("telemetry must not contain raw path", text.contains(rawPath))
    assertFalse(
        "telemetry must not match denylist",
        RedactionFilter.containsDeniedContent(text),
    )
  }

  private fun assertCrashPayloadMustNotContainRawPath(
      payload: Map<String, Any?>,
      rawPath: String,
  ) {
    payload.values.forEach { value -> assertValueMustNotContainRawPath(value, rawPath) }
  }

  private fun assertValueMustNotContainRawPath(value: Any?, rawPath: String) {
    when (value) {
      null -> Unit
      is String -> assertTextMustNotContainRawPath(value, rawPath)
      is Map<*, *> ->
          value.values.forEach { nested -> assertValueMustNotContainRawPath(nested, rawPath) }
      is List<*> ->
          value.forEach { nested -> assertValueMustNotContainRawPath(nested, rawPath) }
      else -> Unit
    }
  }
}
