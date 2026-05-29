package com.dupbuster.scanengine.discovery

import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode

/**
 * Inputs for Mode B platform discovery (requirements §5.1 mode B).
 *
 * @param additionalSafGrants Optional SAF trees previously granted by the user (union).
 */
data class PlatformDiscoveryRequest(
    val scanRootId: Long,
    val generation: Int,
    val grant: ScanRootGrant,
    val additionalSafGrants: List<ScanRootGrant> = emptyList(),
) {
  init {
    require(grant.mode == ScanRootMode.PLATFORM_DISCOVERY) {
      "PlatformDiscoveryRequest requires ScanRootMode.PLATFORM_DISCOVERY"
    }
    additionalSafGrants.forEach { safGrant ->
      require(safGrant.mode == ScanRootMode.USER_SELECTED) {
        "additionalSafGrants must be ScanRootMode.USER_SELECTED SAF trees"
      }
    }
  }
}
