package com.dupbuster.scanengine.discovery

import android.net.Uri
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode

/** Marker [ScanRootGrant] for Mode B runs (`scan_root.uri_or_grant`). */
object PlatformDiscoveryGrant {
  val MARKER_URI: Uri = Uri.parse("content://dupbuster/scan-root/platform-discovery")

  fun grant(): ScanRootGrant =
      ScanRootGrant(uriGrant = MARKER_URI, mode = ScanRootMode.PLATFORM_DISCOVERY)
}
