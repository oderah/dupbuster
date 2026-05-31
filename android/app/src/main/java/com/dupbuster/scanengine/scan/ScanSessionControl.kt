package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.DiscoveryEntryConsumer
import com.dupbuster.scanengine.discovery.DiscoveryResult
import com.dupbuster.scanengine.stat.StagedFile

/** Cooperative cancel / pause gate checked by the orchestrator pipeline. */
class ScanSessionControl {
  @Volatile var cancelRequested: Boolean = false

  @Volatile var paused: Boolean = false

  fun isCancelled(): Boolean = cancelRequested

  fun awaitIfPaused() {
    while (paused && !cancelRequested) {
      Thread.sleep(20)
    }
  }
}

fun stagedFromDiscovered(entry: DiscoveredEntry): StagedFile =
    StagedFile(
        discovered = entry,
        sizeBytes = entry.sizeBytes,
        mtimeNs = entry.mtimeNs,
        inode = null,
        deviceId = null,
        isSymlink = false,
        mediaTypeHint = entry.mediaTypeHint,
    )

fun interface ScanDiscoveryRunner {
  fun discover(
      request: ScanStartRequest,
      plan: ScanRootResolver.ResolvedPlan,
      generation: Int,
      consumer: DiscoveryEntryConsumer,
      isCancelled: () -> Boolean,
  ): DiscoveryResult
}
