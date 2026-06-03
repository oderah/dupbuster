package com.dupbuster.scanengine.fixtures

import android.content.Context
import android.net.Uri
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.security.RedactionFilter
import com.dupbuster.scanengine.security.SafUriRules
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.security.ScanTelemetryEgress
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.security.UriProvenance
import com.dupbuster.scanengine.security.UriValidationResult
import com.dupbuster.scanengine.security.UriValidator
import com.dupbuster.scanengine.stat.FileStatReader
import com.dupbuster.scanengine.stat.FileStatReadOutcome
import com.dupbuster.scanengine.stat.StatResult
import com.dupbuster.scanengine.stat.StatStage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/** Runs M4 CI security gates from fixture `expect.ciGate` descriptors. */
object SecurityCiGateRunner {

  fun runRedact(fixture: JSONObject) {
    when (fixture.getString("id")) {
      "security-redact-01" -> runSecurityRedact01(fixture)
      else -> error("Unhandled redact ciGate fixture: ${fixture.getString("id")}")
    }
  }

  fun runUri(fixture: JSONObject, context: Context) {
    when (fixture.getString("id")) {
      "security-uri-01" -> runSecurityUri01(fixture, context)
      else -> error("Unhandled uri ciGate fixture: ${fixture.getString("id")}")
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

  private fun runSecurityUri01(fixture: JSONObject, context: Context) {
    val input = fixture.getJSONObject("input")
    val expect = fixture.getJSONObject("expect")
    require(expect.getBoolean("ciGate")) { "fixture must set expect.ciGate" }
    require(expect.getString("validation") == "denied") {
      "fixture must expect validation denied"
    }
    require(expect.getString("unscannableReason") == "PERMISSION_DENIED") {
      "fixture must expect PERMISSION_DENIED"
    }
    require(expect.getBoolean("mustNotOpen")) { "fixture must set expect.mustNotOpen" }

    val treeGrantUri = Uri.parse(input.getString("treeGrant"))
    val candidateUri = Uri.parse(input.getString("candidate"))
    val provenance =
        when (input.getString("provenance")) {
          "discovery" -> UriProvenance.DISCOVERY
          else -> error("Unhandled provenance: ${input.getString("provenance")}")
        }
    val grant = ScanRootGrant(uriGrant = treeGrantUri, mode = ScanRootMode.USER_SELECTED)
    val validator = UriValidator(context)

    val documentId =
        SafUriRules.extractDocumentIdFromUriPath(candidateUri.path)
            ?: error("fixture candidate must yield a SAF document id")
    assertTrue(
        "crafted docId must contain traversal segment",
        SafUriRules.documentIdHasTraversalSegment(documentId),
    )

    val validation = validator.validate(candidateUri, grant, provenance)
    assertTrue(validation is UriValidationResult.Denied)
    assertEquals(
        UnscannableReason.PERMISSION_DENIED,
        (validation as UriValidationResult.Denied).reason,
    )

    val fakeReader =
        object : FileStatReader {
          override fun readStat(uri: Uri): FileStatReadOutcome {
            throw AssertionError("open must not run when UriValidator denies (AC-security-uri-01)")
          }
        }
    val stage = StatStage(context, fileStatReader = fakeReader, uriValidator = validator)
    val entry =
        DiscoveredEntry(
            contentUri = candidateUri,
            scanRootId = 1L,
            generation = 1,
            displayName = "secret",
            mediaTypeHint = MediaTypeHint.OTHER,
            sizeBytes = 0L,
            mtimeNs = 0L,
        )
    val statResult = stage.stat(entry, grant, provenance)
    assertTrue(statResult is StatResult.Unscannable)
    assertEquals(
        UnscannableReason.PERMISSION_DENIED,
        (statResult as StatResult.Unscannable).reason,
    )
  }
}
