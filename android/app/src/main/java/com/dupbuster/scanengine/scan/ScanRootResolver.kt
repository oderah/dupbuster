package com.dupbuster.scanengine.scan

import android.net.Uri
import com.dupbuster.scanengine.discovery.PlatformDiscoveryGrant
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode

object ScanRootResolver {

  data class ResolvedPlan(
      val primaryRootId: Long,
      val roots: List<ResolvedScanRoot>,
      val platformGrant: ScanRootGrant?,
      val additionalSafGrants: List<ScanRootGrant>,
  )

  fun resolve(request: ScanStartRequest, indexWriter: IndexWriter): ResolvedPlan {
    return when (request.mode) {
      ScanRootMode.PLATFORM_DISCOVERY -> resolvePlatformDiscovery(request, indexWriter)
      ScanRootMode.USER_SELECTED -> resolveUserSelected(request, indexWriter)
    }
  }

  private fun resolvePlatformDiscovery(
      request: ScanStartRequest,
      indexWriter: IndexWriter,
  ): ResolvedPlan {
    val platformUri = PlatformDiscoveryGrant.MARKER_URI.toString()
    val platformRootId =
        indexWriter.findOrInsertScanRoot(
            uriOrGrant = platformUri,
            mode = ScanRootMode.PLATFORM_DISCOVERY,
        )
    val resolvedAdditional =
        request.roots.map { root ->
          val rootId =
              root.scanRootId
                  ?: indexWriter.findOrInsertScanRoot(
                      uriOrGrant = root.uriGrant,
                      mode = ScanRootMode.USER_SELECTED,
                  )
          ResolvedScanRoot(
              scanRootId = rootId,
              uriGrant = root.uriGrant,
              mode = ScanRootMode.USER_SELECTED,
          )
        }
    val safGrants =
        resolvedAdditional.map { resolved ->
          ScanRootGrant(
              uriGrant = Uri.parse(resolved.uriGrant),
              mode = ScanRootMode.USER_SELECTED,
          )
        }
    return ResolvedPlan(
        primaryRootId = platformRootId,
        roots =
            listOf(
                ResolvedScanRoot(
                    scanRootId = platformRootId,
                    uriGrant = platformUri,
                    mode = ScanRootMode.PLATFORM_DISCOVERY,
                ),
            ) + resolvedAdditional,
        platformGrant = PlatformDiscoveryGrant.grant(),
        additionalSafGrants = safGrants,
    )
  }

  private fun resolveUserSelected(
      request: ScanStartRequest,
      indexWriter: IndexWriter,
  ): ResolvedPlan {
    if (request.roots.isEmpty()) {
      throw IllegalArgumentException("user_selected scan requires at least one root grant")
    }
    val resolved =
        request.roots.map { root ->
          val rootId =
              root.scanRootId
                  ?: indexWriter.findOrInsertScanRoot(
                      uriOrGrant = root.uriGrant,
                      mode = ScanRootMode.USER_SELECTED,
                  )
          ResolvedScanRoot(
              scanRootId = rootId,
              uriGrant = root.uriGrant,
              mode = ScanRootMode.USER_SELECTED,
          )
        }
    return ResolvedPlan(
        primaryRootId = resolved.first().scanRootId,
        roots = resolved,
        platformGrant = null,
        additionalSafGrants = emptyList(),
    )
  }
}
