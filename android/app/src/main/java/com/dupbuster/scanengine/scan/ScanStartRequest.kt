package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.security.ScanRootMode

/** Parsed `startScan` options from the RN bridge (NativeScanEngine.ts). */
data class ScanStartRequest(
    val mode: ScanRootMode,
    val roots: List<ScanRootInput>,
    val resumeScanRunId: Long? = null,
    val largeFilesOptIn: Boolean = false,
)

data class ScanRootInput(
    val uriGrant: String,
    val scanRootId: Long? = null,
)

/** Active scan_root row + grant context for discovery/stat. */
data class ResolvedScanRoot(
    val scanRootId: Long,
    val uriGrant: String,
    val mode: ScanRootMode,
)
