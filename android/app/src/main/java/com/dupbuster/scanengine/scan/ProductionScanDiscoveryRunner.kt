package com.dupbuster.scanengine.scan

import android.content.Context
import com.dupbuster.scanengine.discovery.DiscoveryEmitter
import com.dupbuster.scanengine.discovery.DiscoveryRequest
import com.dupbuster.scanengine.discovery.PlatformDiscoveryRequest
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import android.net.Uri

class ProductionScanDiscoveryRunner(context: Context) : ScanDiscoveryRunner {

  private val discoveryEmitter = DiscoveryEmitter(context)

  override fun discover(
      request: ScanStartRequest,
      plan: ScanRootResolver.ResolvedPlan,
      generation: Int,
      consumer: com.dupbuster.scanengine.discovery.DiscoveryEntryConsumer,
      isCancelled: () -> Boolean,
  ): com.dupbuster.scanengine.discovery.DiscoveryResult {
    return when (request.mode) {
      ScanRootMode.PLATFORM_DISCOVERY -> {
        val platformGrant =
            plan.platformGrant
                ?: throw IllegalStateException("platform_discovery plan missing platform grant")
        val platformRoot = plan.roots.first { it.mode == ScanRootMode.PLATFORM_DISCOVERY }
        discoveryEmitter.emitModeB(
            PlatformDiscoveryRequest(
                scanRootId = platformRoot.scanRootId,
                generation = generation,
                grant = platformGrant,
                additionalSafGrants = plan.additionalSafGrants,
            ),
            consumer,
            isCancelled,
        )
      }
      ScanRootMode.USER_SELECTED -> {
        var totalEmitted = 0
        var totalDenied = 0
        var totalDirs = 0
        var cancelled = false
        for (root in plan.roots) {
          if (isCancelled()) {
            cancelled = true
            break
          }
          val result =
              discoveryEmitter.emitModeA(
                  DiscoveryRequest(
                      scanRootId = root.scanRootId,
                      generation = generation,
                      grant =
                          ScanRootGrant(
                              uriGrant = Uri.parse(root.uriGrant),
                              mode = ScanRootMode.USER_SELECTED,
                          ),
                  ),
                  consumer,
                  isCancelled,
              )
          totalEmitted += result.entriesEmitted
          totalDenied += result.entriesDenied
          totalDirs += result.directoriesVisited
          cancelled = cancelled || result.cancelled
          if (cancelled) {
            break
          }
        }
        com.dupbuster.scanengine.discovery.DiscoveryResult(
            entriesEmitted = totalEmitted,
            entriesDenied = totalDenied,
            directoriesVisited = totalDirs,
            cancelled = cancelled,
        )
      }
    }
  }
}
