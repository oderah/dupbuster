package com.dupbuster.scanengine.discovery

/** One row from a SAF children query (testable without a live provider). */
data class SafChildRow(
    val documentId: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val isDirectory: Boolean,
)
