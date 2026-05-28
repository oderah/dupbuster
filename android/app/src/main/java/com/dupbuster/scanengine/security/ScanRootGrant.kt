package com.dupbuster.scanengine.security

import android.net.Uri

/**
 * Active [scan_root] grant context for grant-boundary checks (architecture §8.1).
 *
 * @param uriGrant SAF tree URI, or platform-discovery marker URI from `scan_root.uri_or_grant`
 */
data class ScanRootGrant(
    val uriGrant: Uri,
    val mode: ScanRootMode,
)
