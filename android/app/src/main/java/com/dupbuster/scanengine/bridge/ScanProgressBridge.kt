package com.dupbuster.scanengine.bridge

import com.facebook.react.bridge.ReadableMap
import com.dupbuster.scanengine.discovery.MediaTypeHint

/**
 * Throttled `onScanProgress` emitter for the scan orchestrator (M1-16).
 * Coalesces to ≤4 Hz then maps snapshots through [ScanProgressBridgeMapper].
 * Wire from [com.dupbuster.scanengine.ScanEngineModule] via `emitOnScanProgress`.
 */
class ScanProgressBridge(
    private val emitProgress: (ReadableMap) -> Unit,
    private val throttle: ProgressThrottle =
        ProgressThrottle { snapshot, _ ->
          val payload = ScanProgressBridgeMapper.toReadableMap(snapshot)
          ScanProgressBridgeMapper.assertBridgeSafePayload(payload)
          emitProgress(payload)
        },
) {
  fun report(snapshot: ScanProgressSnapshot, atMs: Long) {
    throttle.report(snapshot, atMs)
  }

  fun advanceTo(atMs: Long): Boolean = throttle.advanceTo(atMs)

  fun flush(atMs: Long) = throttle.flush(atMs)

  fun reset() = throttle.reset()

  fun reportHashingProgress(
      filesProcessed: Int,
      filesTotalKnown: Int?,
      groupsFound: Int,
      reclaimableBytesEst: Long,
      mediaTypeHint: MediaTypeHint,
      isVideoFingerprintPass: Boolean,
      atMs: Long,
  ) {
    val contentKind =
        ScanProgressContentKindPolicy.forHashingPhase(mediaTypeHint, isVideoFingerprintPass)
    report(
        ScanProgressSnapshot(
            filesProcessed = filesProcessed,
            filesTotalKnown = filesTotalKnown,
            groupsFound = groupsFound,
            reclaimableBytesEst = reclaimableBytesEst,
            phase = ScanPhase.HASHING,
            contentKind = contentKind,
        ),
        atMs,
    )
  }
}
