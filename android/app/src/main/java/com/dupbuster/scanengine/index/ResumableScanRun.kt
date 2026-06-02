package com.dupbuster.scanengine.index

/** Bridge-facing interrupted scan metadata (`getResumableScanRun`). */
data class ResumableScanRun(
    val scanRunId: Long,
    val lastProcessedId: Long,
    val status: String,
)
