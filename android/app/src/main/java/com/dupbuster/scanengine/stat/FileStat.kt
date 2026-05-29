package com.dupbuster.scanengine.stat

/**
 * Authoritative file metadata from StatStage (architecture §3.2 / `file_entry` columns).
 * Video duration/dimensions populated for `media_type=video` (M1-13+).
 */
data class FileStat(
    val sizeBytes: Long,
    val mtimeNs: Long,
    val inode: Long?,
    val deviceId: Long?,
    val isSymlink: Boolean,
    val durationMs: Long? = null,
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
)
