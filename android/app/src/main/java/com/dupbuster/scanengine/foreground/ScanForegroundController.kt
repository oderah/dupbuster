package com.dupbuster.scanengine.foreground

import com.dupbuster.scanengine.bridge.ScanPhase
import com.dupbuster.scanengine.bridge.ScanProgressSnapshot

/** Lifecycle hook for Android scan foreground service (M4-01). */
interface ScanForegroundController {
  fun onProgress(snapshot: ScanProgressSnapshot, atMs: Long)
}

/** Headless / unit-test no-op. */
object NoOpScanForegroundController : ScanForegroundController {
  override fun onProgress(snapshot: ScanProgressSnapshot, atMs: Long) = Unit
}

/**
 * Maps orchestrator progress to FGS start / update / stop.
 * Notification updates coalesce to 250 ms; terminal phases flush immediately.
 */
class AndroidScanForegroundController(
    private val serviceClient: ScanForegroundServiceClient,
    private val minUpdateIntervalMs: Long = 250L,
) : ScanForegroundController {
  private var foregroundActive: Boolean = false
  private var lastUpdateMs: Long = 0L
  private var pendingSnapshot: ScanProgressSnapshot? = null

  override fun onProgress(snapshot: ScanProgressSnapshot, atMs: Long) {
    if (isTerminalPhase(snapshot.phase)) {
      pendingSnapshot = null
      if (foregroundActive) {
        serviceClient.stop()
        foregroundActive = false
      }
      lastUpdateMs = 0L
      return
    }

    if (!foregroundActive) {
      serviceClient.start()
      foregroundActive = true
      lastUpdateMs = atMs
      serviceClient.update(snapshot.filesProcessed, snapshot.filesTotalKnown)
      return
    }

    if (atMs - lastUpdateMs >= minUpdateIntervalMs) {
      lastUpdateMs = atMs
      serviceClient.update(snapshot.filesProcessed, snapshot.filesTotalKnown)
      pendingSnapshot = null
      return
    }

    pendingSnapshot = snapshot
  }

  fun flushPending(atMs: Long) {
    val snapshot = pendingSnapshot ?: return
    if (!foregroundActive || isTerminalPhase(snapshot.phase)) {
      pendingSnapshot = null
      return
    }
    lastUpdateMs = atMs
    serviceClient.update(snapshot.filesProcessed, snapshot.filesTotalKnown)
    pendingSnapshot = null
  }

  private fun isTerminalPhase(phase: String): Boolean =
      phase == ScanPhase.COMPLETE ||
          phase == ScanPhase.ERROR ||
          phase == ScanPhase.CANCELLED ||
          phase == ScanPhase.IDLE
}
