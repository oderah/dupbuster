package com.dupbuster.scanengine.index

/** In-memory view of a `scan_run` row for orchestrator / resume (FR-SI-03). */
data class ScanRunSnapshot(
    val id: Long,
    val rootId: Long?,
    val generation: Int,
    val status: String,
    val lastProcessedId: Long,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val teardownReason: String?,
)
