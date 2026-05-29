package com.dupbuster.scanengine.bridge

/**
 * Coalesces scan progress to the bridge contract: max 4 events/s (250 ms minimum interval).
 * Orchestrator calls [report] on each pipeline tick; [advanceTo] / [flush] drain pending snapshots.
 */
class ProgressThrottle(
    private val minIntervalMs: Long = MIN_INTERVAL_MS,
    private val onEmit: (ScanProgressSnapshot, emittedAtMs: Long) -> Unit,
) {
  private var lastEmitAtMs: Long = Long.MIN_VALUE
  private var pending: ScanProgressSnapshot? = null

  /** Latest-wins coalesce; emits immediately when [atMs] - last emit ≥ [minIntervalMs]. */
  fun report(snapshot: ScanProgressSnapshot, atMs: Long) {
    pending = snapshot
    if (lastEmitAtMs == Long.MIN_VALUE || atMs - lastEmitAtMs >= minIntervalMs) {
      emitNow(atMs)
    }
  }

  /** Emit pending snapshot if the coalesce window has elapsed (call when virtual/real time advances). */
  fun advanceTo(atMs: Long): Boolean {
    if (pending == null) {
      return false
    }
    if (lastEmitAtMs != Long.MIN_VALUE && atMs - lastEmitAtMs < minIntervalMs) {
      return false
    }
    emitNow(atMs)
    return true
  }

  /** Force-emit the latest pending snapshot (e.g. scan terminal state). */
  fun flush(atMs: Long) {
    if (pending != null) {
      emitNow(atMs)
    }
  }

  fun reset() {
    lastEmitAtMs = Long.MIN_VALUE
    pending = null
  }

  fun hasPending(): Boolean = pending != null

  private fun emitNow(atMs: Long) {
    val snapshot = pending ?: return
    onEmit(snapshot, atMs)
    lastEmitAtMs = atMs
    pending = null
  }

  companion object {
    const val MIN_INTERVAL_MS: Long = 250L
    const val MAX_EVENTS_PER_SECOND: Int = 4
  }
}
