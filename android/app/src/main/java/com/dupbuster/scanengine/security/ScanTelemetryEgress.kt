package com.dupbuster.scanengine.security

/**
 * Single egress point for opt-in crash SDK and analytics strings (FR-SE-02). Catalog UI paths stay
 * outside this layer.
 */
object ScanTelemetryEgress {

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
}
