package com.dupbuster.scanengine.index

import com.dupbuster.scanengine.security.ScanRootGrant

/** Resolved catalog row for delete validation + platform API (M3-03). */
data class FileEntryDeleteTarget(
    val fileEntryId: Long,
    val uriOrPath: String,
    val grant: ScanRootGrant,
)
