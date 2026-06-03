package com.dupbuster.scanengine.security

/**
 * Closed allowlist for opt-in crash/analytics egress (architecture §8.2). Unscannable reason codes,
 * paths, hashes, and debug blobs are never permitted off-device in v1.
 */
object TelemetryAllowedFields {

  val ALLOWED_KEYS: Set<String> =
      setOf(
          "scan_run_id",
          "phase",
          "files_processed",
          "groups_found",
          "schema_version",
          "teardown_reason",
          "platform_api_level",
          "exception_type",
      )

  val BANNED_EGRESS_KEYS: Set<String> =
      setOf(
          "unscannable_reason",
          "uri_or_path",
          "display_name",
          "path",
          "paths",
          "hash_value",
          "hash_algo",
          "frame_hashes_blob",
          "normalization_profile",
          "thumbnail_uri",
          "thumbnailUri",
      )

  fun filterToAllowed(payload: Map<String, Any?>): Map<String, Any?> =
      payload.filterKeys { key ->
        key in ALLOWED_KEYS && key !in BANNED_EGRESS_KEYS
      }
}
