package com.dupbuster.scanengine.discovery

import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode

/** Inputs for a single Mode A discovery pass (requirements §5.1 mode A). */
data class DiscoveryRequest(
    val scanRootId: Long,
    val generation: Int,
    val grant: ScanRootGrant,
) {
  init {
    require(grant.mode == ScanRootMode.USER_SELECTED) {
      "DiscoveryRequest Mode A requires ScanRootMode.USER_SELECTED"
    }
  }
}
