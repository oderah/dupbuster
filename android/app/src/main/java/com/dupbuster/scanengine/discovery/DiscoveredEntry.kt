package com.dupbuster.scanengine.discovery

import android.net.Uri

/**
 * One file discovered under an active scan_root (architecture §4.1 stage 1).
 * URIs stay native-side until IndexWriter persists them — never sent on the RN bridge.
 */
data class DiscoveredEntry(
    val contentUri: Uri,
    val scanRootId: Long,
    val generation: Int,
    val displayName: String,
    val mediaTypeHint: MediaTypeHint,
    val sizeBytes: Long,
    val mtimeNs: Long,
)
