package com.dupbuster.scanengine.stat

/**
 * Authoritative file metadata from StatStage (architecture §3.2 / `file_entry` columns).
 * Video duration/dimensions are added in a later milestone (M1-13+).
 */
data class FileStat(
    val sizeBytes: Long,
    val mtimeNs: Long,
    val inode: Long?,
    val deviceId: Long?,
    val isSymlink: Boolean,
)
