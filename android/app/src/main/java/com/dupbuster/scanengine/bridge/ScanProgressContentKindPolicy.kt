package com.dupbuster.scanengine.bridge

import com.dupbuster.scanengine.discovery.MediaTypeHint

/**
 * Resolves optional `contentKind` for hashing-phase progress (M1-16 / AC-integrity-progress-02).
 * Non-hashing phases omit the field on the bridge.
 */
object ScanProgressContentKindPolicy {

  /**
   * @param isVideoFingerprintPass `true` while `VIDEO_CONTENT_V1` fingerprinting is active for the
   *   current file (parallel RAW_BYTES may still run).
   */
  fun forHashingPhase(isVideoFingerprintPass: Boolean): String =
      if (isVideoFingerprintPass) {
        ScanProgressContentKind.VIDEO_CONTENT
      } else {
        ScanProgressContentKind.NONE
      }

  /** Convenience when pipeline knows [MediaTypeHint] and which hash pass is running. */
  fun forHashingPhase(
      mediaTypeHint: MediaTypeHint,
      isVideoFingerprintPass: Boolean,
  ): String? {
    if (mediaTypeHint != MediaTypeHint.VIDEO && !isVideoFingerprintPass) {
      return ScanProgressContentKind.NONE
    }
    return forHashingPhase(isVideoFingerprintPass)
  }

  fun bridgeContentKind(phase: String, snapshotContentKind: String?): String? {
    if (phase != ScanPhase.HASHING) {
      return null
    }
    return when (snapshotContentKind) {
      null -> null
      ScanProgressContentKind.NONE,
      ScanProgressContentKind.VIDEO_CONTENT,
      -> snapshotContentKind
      else -> throw IllegalArgumentException("Invalid contentKind: $snapshotContentKind")
    }
  }

  fun isAllowedBridgeValue(value: String): Boolean =
      value == ScanProgressContentKind.NONE || value == ScanProgressContentKind.VIDEO_CONTENT
}
