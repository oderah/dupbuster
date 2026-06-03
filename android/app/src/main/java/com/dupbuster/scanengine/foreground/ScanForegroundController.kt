package com.dupbuster.scanengine.foreground

import com.dupbuster.scanengine.bridge.ScanPhase
import com.dupbuster.scanengine.bridge.ScanProgressSnapshot
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Lifecycle hook for Android scan foreground service (M4-01 / M4-02). */
interface ScanForegroundController {
  fun onProgress(snapshot: ScanProgressSnapshot, atMs: Long)

  /** Called when user cancel is requested; FGS must clear within hard bound (M4-02). */
  fun onCancelRequested(atMs: Long) = Unit
}

/** Headless / unit-test no-op. */
object NoOpScanForegroundController : ScanForegroundController {
  override fun onProgress(snapshot: ScanProgressSnapshot, atMs: Long) = Unit
}

/**
 * Maps orchestrator progress to FGS start / update / stop.
 * Notification updates coalesce to 250 ms; terminal phases flush immediately.
 * Cancel teardown keeps FGS during [ScanPhase.CANCELLING] and enforces 120 s hard stop (M4-02).
 */
class AndroidScanForegroundController(
    private val serviceClient: ScanForegroundServiceClient,
    private val minUpdateIntervalMs: Long = 250L,
    private val notificationHardBoundMs: Long = ScanCancelTeardownConstants.NOTIFICATION_HARD_BOUND_MS,
    private val cancelWatchdogExecutor: Executor =
        Executors.newSingleThreadExecutor { runnable ->
          Thread(runnable, "dupbuster-fgs-cancel-watchdog").apply { isDaemon = true }
        },
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) : ScanForegroundController {
  private var foregroundActive: Boolean = false
  private var lastUpdateMs: Long = 0L
  private var pendingSnapshot: ScanProgressSnapshot? = null
  @Volatile private var cancelHardBoundWatchActive: Boolean = false

  override fun onCancelRequested(atMs: Long) {
    if (!foregroundActive || cancelHardBoundWatchActive) {
      return
    }
    cancelHardBoundWatchActive = true
    val hardStopAtMs = atMs + notificationHardBoundMs
    cancelWatchdogExecutor.execute {
      while (clock() < hardStopAtMs) {
        if (!foregroundActive) {
          cancelHardBoundWatchActive = false
          return@execute
        }
        sleeper(100L)
      }
      if (foregroundActive) {
        serviceClient.stop()
        foregroundActive = false
      }
      cancelHardBoundWatchActive = false
    }
  }

  override fun onProgress(snapshot: ScanProgressSnapshot, atMs: Long) {
    if (isTerminalPhase(snapshot.phase)) {
      pendingSnapshot = null
      cancelHardBoundWatchActive = false
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
