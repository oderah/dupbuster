package com.dupbuster.scanengine.foreground

import com.dupbuster.scanengine.bridge.ScanPhase
import com.dupbuster.scanengine.bridge.ScanProgressSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidScanForegroundControllerTest {

  private lateinit var client: RecordingScanForegroundServiceClient
  private lateinit var controller: AndroidScanForegroundController

  @Before
  fun setUp() {
    client = RecordingScanForegroundServiceClient()
    controller = AndroidScanForegroundController(client, minUpdateIntervalMs = 250L)
  }

  @Test
  fun discoveringPhase_startsForegroundAndUpdates() {
    controller.onProgress(discoveringSnapshot(), atMs = 1_000L)

    assertTrue(client.started)
    assertEquals(listOf(0 to null), client.updates)
  }

  @Test
  fun hashingUpdates_coalesceWithin250Ms() {
    controller.onProgress(discoveringSnapshot(), atMs = 1_000L)
    controller.onProgress(hashingSnapshot(filesProcessed = 10), atMs = 1_100L)
    controller.onProgress(hashingSnapshot(filesProcessed = 20), atMs = 1_200L)

    assertEquals(listOf(0 to null), client.updates)
  }

  @Test
  fun hashingUpdates_emitAfterInterval() {
    controller.onProgress(discoveringSnapshot(), atMs = 1_000L)
    controller.onProgress(hashingSnapshot(filesProcessed = 10), atMs = 1_300L)

    assertEquals(listOf(0 to null, 10 to 100), client.updates)
  }

  @Test
  fun completePhase_stopsForeground() {
    controller.onProgress(discoveringSnapshot(), atMs = 1_000L)
    controller.onProgress(completeSnapshot(), atMs = 1_500L)

    assertTrue(client.stopped)
  }

  @Test
  fun cancellingPhase_keepsForegroundActive() {
    controller.onProgress(discoveringSnapshot(), atMs = 1_000L)
    controller.onProgress(
        hashingSnapshot(filesProcessed = 40).copy(phase = ScanPhase.CANCELLING),
        atMs = 1_200L,
    )

    assertTrue(client.started)
    org.junit.Assert.assertFalse(client.stopped)
  }

  @Test
  fun cancelHardBound_forceStopsForegroundWithin120Seconds() {
    val clock = object {
      var now = 0L
      fun get(): Long = now
    }
    val controllerWithWatchdog =
        AndroidScanForegroundController(
            client,
            minUpdateIntervalMs = 250L,
            notificationHardBoundMs = 120_000L,
            cancelWatchdogExecutor = java.util.concurrent.Executor { it.run() },
            clock = clock::get,
            sleeper = { ms -> clock.now += ms },
        )

    controllerWithWatchdog.onProgress(discoveringSnapshot(), atMs = 0L)
    controllerWithWatchdog.onCancelRequested(atMs = 0L)

    assertTrue(client.stopped)
  }

  @Test
  fun cancelledPhase_stopsForegroundAfterCancelling() {
    controller.onProgress(discoveringSnapshot(), atMs = 1_000L)
    controller.onProgress(
        hashingSnapshot(filesProcessed = 40).copy(phase = ScanPhase.CANCELLING),
        atMs = 1_200L,
    )
    controller.onCancelRequested(atMs = 1_200L)
    controller.onProgress(
        hashingSnapshot(filesProcessed = 40).copy(phase = ScanPhase.CANCELLED),
        atMs = 1_500L,
    )

    assertTrue(client.stopped)
  }

  private fun discoveringSnapshot() =
      ScanProgressSnapshot(
          filesProcessed = 0,
          filesTotalKnown = null,
          groupsFound = 0,
          reclaimableBytesEst = 0,
          phase = ScanPhase.DISCOVERING,
      )

  private fun hashingSnapshot(filesProcessed: Int) =
      ScanProgressSnapshot(
          filesProcessed = filesProcessed,
          filesTotalKnown = 100,
          groupsFound = 0,
          reclaimableBytesEst = 0,
          phase = ScanPhase.HASHING,
      )

  private fun completeSnapshot() =
      ScanProgressSnapshot(
          filesProcessed = 100,
          filesTotalKnown = 100,
          groupsFound = 2,
          reclaimableBytesEst = 1024,
          phase = ScanPhase.COMPLETE,
      )

  private class RecordingScanForegroundServiceClient : ScanForegroundServiceClient {
    var started: Boolean = false
    var stopped: Boolean = false
    val updates = mutableListOf<Pair<Int, Int?>>()

    override fun start() {
      started = true
    }

    override fun update(filesProcessed: Int, filesTotalKnown: Int?) {
      updates.add(filesProcessed to filesTotalKnown)
    }

    override fun stop() {
      stopped = true
    }
  }
}
