package com.dupbuster.scanengine.security

import org.json.JSONArray
import org.json.JSONObject

/**
 * Denylist redaction before crash SDK, opt-in analytics, and JS bridge error egress (FR-SE-02,
 * architecture §8.2). On-screen catalog paths are not passed through this filter.
 */
object RedactionFilter {

  const val REDACTED_PLACEHOLDER: String = "[REDACTED]"

  /** Pattern 1 — extended to full path segment so AC-security-redact-01 holds. */
  private val PATTERN_PLATFORM_PREFIX =
      Regex(
          """(?i)(/storage/[^\s"']+|/sdcard/[^\s"']+|/data/user/[^\s"']+|/var/mobile/[^\s"']+|/private/var/[^\s"']+)""",
      )

  private val PATTERN_CONTENT_URI = Regex("""(?i)content://[^\s"']+""")
  private val PATTERN_FILE_URI = Regex("""(?i)file://[^\s"']+""")
  private val PATTERN_PHOTO_LIBRARY_URI = Regex("""(?i)(ph://|assets-library://)[^\s"']+""")
  private val PATTERN_USER_HOME =
      Regex("""(?i)([A-Za-z]:\\Users\\[^\s"']+|/Users/[^\s"']+|/home/[^\s"']+)""")
  private val PATTERN_MEDIA_PATH_SEGMENT =
      Regex(
          """(?i)([/\\][^\s"']+\.(?:jpg|jpeg|png|gif|webp|heic|heif|bmp|tiff|tif|mp4|mov|m4v|avi|mkv|webm|mp3|m4a|wav|aac|flac|pdf|txt|doc|docx|xls|xlsx|ppt|pptx))""",
      )

  private val DENYLIST_PATTERNS: List<Regex> =
      listOf(
          PATTERN_PLATFORM_PREFIX,
          PATTERN_CONTENT_URI,
          PATTERN_FILE_URI,
          PATTERN_PHOTO_LIBRARY_URI,
          PATTERN_USER_HOME,
          PATTERN_MEDIA_PATH_SEGMENT,
      )

  /** Pattern 7 — structured crash / analytics payload keys (never bridge catalog fields). */
  val CRASH_SENSITIVE_KEYS: Set<String> =
      setOf(
          "uri_or_path",
          "display_name",
          "path",
          "paths",
          "thumbnailUri",
          "thumbnail_uri",
          "alias_path",
          "alias_paths",
      )

  fun apply(text: String?): String? {
    if (text.isNullOrEmpty()) {
      return text
    }
    var result: String = text
    for (pattern in DENYLIST_PATTERNS) {
      result = pattern.replace(result, REDACTED_PLACEHOLDER)
    }
    return result
  }

  fun containsDeniedContent(text: String?): Boolean {
    if (text.isNullOrEmpty()) {
      return false
    }
    return DENYLIST_PATTERNS.any { it.containsMatchIn(text) }
  }

  fun applyToCrashPayload(payload: Map<String, Any?>): Map<String, Any?> =
      redactStructuredValue(payload) as Map<String, Any?>

  fun applyToJsonObject(payload: JSONObject): JSONObject {
    val copy = JSONObject(payload.toString())
    redactJsonValue(copy)
    return copy
  }

  private fun redactStructuredValue(value: Any?): Any? =
      when (value) {
        null -> null
        is String -> apply(value)
        is Map<*, *> -> {
          val out = linkedMapOf<String, Any?>()
          value.forEach { (key, nested) ->
            val keyString = key?.toString() ?: return@forEach
            out[keyString] = redactStructuredEntry(keyString, nested)
          }
          out
        }
        is List<*> -> value.map { redactStructuredValue(it) }
        else -> value
      }

  private fun redactStructuredEntry(key: String, value: Any?): Any? {
    if (key in CRASH_SENSITIVE_KEYS && value != null) {
      return REDACTED_PLACEHOLDER
    }
    return redactStructuredValue(value)
  }

  private fun redactJsonValue(value: Any?) {
    when (value) {
      is JSONObject -> {
        val keys = value.keys().asSequence().toList()
        for (key in keys) {
          val nested = value.opt(key)
          if (key in CRASH_SENSITIVE_KEYS && nested != null && nested != JSONObject.NULL) {
            value.put(key, REDACTED_PLACEHOLDER)
          } else {
            redactJsonValue(nested)
            if (nested is String) {
              value.put(key, apply(nested))
            }
          }
        }
      }
      is JSONArray -> {
        for (index in 0 until value.length()) {
          val nested = value.opt(index)
          redactJsonValue(nested)
          if (nested is String) {
            value.put(index, apply(nested))
          }
        }
      }
    }
  }
}
