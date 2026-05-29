package com.dupbuster.scanengine.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressThrottleTest {

  @Test
  fun report_10kRapidUpdates_atMost4EventsPerSecond_acIntegrityProgress01() {
    val emitTimesMs = mutableListOf<Long>()
    val throttle =
        ProgressThrottle { _, emittedAtMs ->
          emitTimesMs.add(emittedAtMs)
        }

    var t = 0L
    repeat(10_000) {
      throttle.report(progress(filesProcessed = it), atMs = t)
      throttle.advanceTo(atMs = t)
      t += 1
    }
    throttle.flush(atMs = t)

    assertTrue(emitTimesMs.isNotEmpty())
    val maxPerSecond = maxEventsInAnyOneSecondWindow(emitTimesMs)
    assertTrue(
        "Expected ≤ ${ProgressThrottle.MAX_EVENTS_PER_SECOND} emits per 1s window, saw $maxPerSecond",
        maxPerSecond <= ProgressThrottle.MAX_EVENTS_PER_SECOND,
    )
  }

  @Test
  fun report_coalescesToLatestWithinWindow() {
    val emitted = mutableListOf<ScanProgressSnapshot>()
    val throttle = ProgressThrottle { snapshot, _ -> emitted.add(snapshot) }

    throttle.report(progress(filesProcessed = 1), atMs = 0)
    throttle.report(progress(filesProcessed = 2), atMs = 100)
    throttle.report(progress(filesProcessed = 3), atMs = 200)

    assertEquals(1, emitted.size)
    assertEquals(1, emitted[0].filesProcessed)

    throttle.advanceTo(atMs = 250)

    assertEquals(2, emitted.size)
    assertEquals(3, emitted[1].filesProcessed)
  }

  @Test
  fun flush_emitsLatestPendingBeforeWindowElapses() {
    val emitted = mutableListOf<ScanProgressSnapshot>()
    val throttle = ProgressThrottle { snapshot, _ -> emitted.add(snapshot) }

    throttle.report(progress(filesProcessed = 1), atMs = 0)
    throttle.report(progress(filesProcessed = 9), atMs = 50)
    throttle.flush(atMs = 50)

    assertEquals(2, emitted.size)
    assertEquals(1, emitted[0].filesProcessed)
    assertEquals(9, emitted[1].filesProcessed)
  }

  @Test
  fun reset_clearsPendingWithoutEmit() {
    val emitted = mutableListOf<ScanProgressSnapshot>()
    val throttle = ProgressThrottle { snapshot, _ -> emitted.add(snapshot) }

    throttle.report(progress(filesProcessed = 1), atMs = 0)
    throttle.report(progress(filesProcessed = 2), atMs = 10)
    throttle.reset()

    assertEquals(1, emitted.size)
    throttle.advanceTo(atMs = 500)
    assertEquals(1, emitted.size)
  }

  @Test
  fun report_hashingVideoContent_coalescesContentKind_acIntegrityProgress02() {
    val emitted = mutableListOf<ScanProgressSnapshot>()
    val throttle = ProgressThrottle { snapshot, _ -> emitted.add(snapshot) }

    throttle.report(
        progress(
            filesProcessed = 1,
            phase = ScanPhase.HASHING,
            contentKind = ScanProgressContentKind.VIDEO_CONTENT,
        ),
        atMs = 0,
    )
    throttle.report(
        progress(
            filesProcessed = 2,
            phase = ScanPhase.HASHING,
            contentKind = ScanProgressContentKind.NONE,
        ),
        atMs = 100,
    )
    throttle.advanceTo(atMs = 250)

    assertEquals(2, emitted.size)
    assertEquals(ScanProgressContentKind.VIDEO_CONTENT, emitted[0].contentKind)
    assertEquals(ScanProgressContentKind.NONE, emitted[1].contentKind)
  }

  @Test
  fun report_10kHashingVideoContent_atMost4EventsPerSecond() {
    val emitTimesMs = mutableListOf<Long>()
    val throttle =
        ProgressThrottle { _, emittedAtMs ->
          emitTimesMs.add(emittedAtMs)
        }

    var t = 0L
    repeat(10_000) {
      throttle.report(
          progress(
              filesProcessed = it,
              phase = ScanPhase.HASHING,
              contentKind = ScanProgressContentKind.VIDEO_CONTENT,
          ),
          atMs = t,
      )
      throttle.advanceTo(atMs = t)
      t += 1
    }
    throttle.flush(atMs = t)

    val maxPerSecond = maxEventsInAnyOneSecondWindow(emitTimesMs)
    assertTrue(
        "Expected ≤ ${ProgressThrottle.MAX_EVENTS_PER_SECOND} emits per 1s window, saw $maxPerSecond",
        maxPerSecond <= ProgressThrottle.MAX_EVENTS_PER_SECOND,
    )
  }

  @Test
  fun report_respectsMinIntervalMs() {
    val emitTimesMs = mutableListOf<Long>()
    val throttle = ProgressThrottle { _, atMs -> emitTimesMs.add(atMs) }

    throttle.report(progress(filesProcessed = 1), atMs = 0)
    throttle.report(progress(filesProcessed = 2), atMs = 100)
    throttle.advanceTo(atMs = 249)
    assertEquals(1, emitTimesMs.size)

    throttle.advanceTo(atMs = 250)
    assertEquals(2, emitTimesMs.size)
    assertEquals(250L, emitTimesMs[1])
  }

  private fun progress(
      filesProcessed: Int,
      phase: String = ScanPhase.DISCOVERING,
      contentKind: String? = null,
  ): ScanProgressSnapshot =
      ScanProgressSnapshot(
          filesProcessed = filesProcessed,
          filesTotalKnown = 10_000,
          groupsFound = 0,
          reclaimableBytesEst = 0L,
          phase = phase,
          contentKind = contentKind,
      )

  /** Sliding 1000 ms window — AC-integrity-progress-01. */
  private fun maxEventsInAnyOneSecondWindow(emitTimesMs: List<Long>): Int {
    if (emitTimesMs.isEmpty()) {
      return 0
    }
    val sorted = emitTimesMs.sorted()
    var maxInWindow = 0
    var start = 0
    for (end in sorted.indices) {
      while (sorted[end] - sorted[start] >= 1000) {
        start++
      }
      val count = end - start + 1
      if (count > maxInWindow) {
        maxInWindow = count
      }
    }
    return maxInWindow
  }
}
