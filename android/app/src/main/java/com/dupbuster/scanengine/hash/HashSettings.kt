package com.dupbuster.scanengine.hash

/** Scan-time hash policy (FR-FP-03 large-file opt-in). */
data class HashSettings(
    val largeFilesOptIn: Boolean = false,
)
