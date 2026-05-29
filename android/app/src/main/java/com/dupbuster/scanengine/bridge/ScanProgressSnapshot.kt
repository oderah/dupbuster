package com.dupbuster.scanengine.bridge

/**
 * Native progress payload for the RN bridge (architecture §6.5).
 * No paths, hashes, or file bytes — metadata only.
 */
data class ScanProgressSnapshot(
    val filesProcessed: Int,
    val filesTotalKnown: Int?,
    val groupsFound: Int,
    val reclaimableBytesEst: Long,
    val phase: String,
    val contentKind: String? = null,
)
