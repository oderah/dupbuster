package com.dupbuster.scanengine.stat

import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint

/**
 * Discovery row plus fresh stat output for the hash pipeline (FR-SI-01 TOCTOU baseline).
 */
data class StagedFile(
    val discovered: DiscoveredEntry,
    val sizeBytes: Long,
    val mtimeNs: Long,
    val inode: Long?,
    val deviceId: Long?,
    val isSymlink: Boolean,
    val mediaTypeHint: MediaTypeHint,
)
