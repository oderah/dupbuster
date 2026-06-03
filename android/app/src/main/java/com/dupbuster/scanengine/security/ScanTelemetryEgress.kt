package com.dupbuster.scanengine.security

/**
 * Single egress point for opt-in crash SDK and analytics strings (FR-SE-02). Catalog UI paths stay
 * outside this layer. Egress is disabled until the user opts in via [setCrashAnalyticsOptIn] (M4-13,
 * US-17).
 */
object ScanTelemetryEgress {

  @Volatile private var crashAnalyticsOptIn: Boolean = false

  @Volatile private var lastRecordedPayload: Map<String, Any?> = emptyMap()

  fun isEgressEnabled(): Boolean = crashAnalyticsOptIn

  fun setCrashAnalyticsOptIn(enabled: Boolean) {
    crashAnalyticsOptIn = enabled
    if (!enabled) {
      lastRecordedPayload = emptyMap()
    }
  }

  fun sanitizeCrashMessage(message: String?): String? = RedactionFilter.apply(message)

  fun sanitizeAnalyticsValue(value: String?): String? = RedactionFilter.apply(value)

  fun sanitizeCrashPayload(payload: Map<String, Any?>): Map<String, Any?> =
      RedactionFilter.applyToCrashPayload(payload)

  fun sanitizeExceptionForTelemetry(throwable: Throwable): String {
    val typeName = throwable.javaClass.simpleName
    val message = RedactionFilter.apply(throwable.message) ?: ""
    return if (message.isEmpty()) {
      typeName
    } else {
      "$typeName: $message"
    }
  }

  /**
   * Records a redacted crash/analytics payload when opt-in is enabled. v1 has no third-party SDK;
   * this gate ensures disabled builds perform zero egress (NFR-06).
   */
  fun recordCrashPayload(payload: Map<String, Any?>): Boolean {
    if (!crashAnalyticsOptIn) {
      return false
    }
    val allowed = TelemetryAllowedFields.filterToAllowed(payload)
    if (allowed.isEmpty()) {
      return false
    }
    lastRecordedPayload = sanitizeCrashPayload(allowed)
    return true
  }

  /** Test-only visibility of the last sanitized payload accepted for egress. */
  internal fun lastRecordedPayloadForTests(): Map<String, Any?> = lastRecordedPayload
}
