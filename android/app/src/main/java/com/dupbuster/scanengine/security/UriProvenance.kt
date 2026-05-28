package com.dupbuster.scanengine.security

/**
 * Where a candidate URI originated. UriValidator rejects user-supplied URIs
 * (architecture §8.1 — no pasted URIs).
 */
enum class UriProvenance {
  /** Active scan_root grant URI at scan start. */
  GRANT_ROOT,
  /** Emitted by DiscoveryEmitter for this scan run. */
  DISCOVERY,
  /** Untrusted input — always fail-closed. */
  USER_SUPPLIED,
}
