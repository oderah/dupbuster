package com.dupbuster.scanengine.index

import android.net.Uri
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint

/**
 * Catalog row skipped by size-bucket elimination (no [file_entry.fingerprint_id] yet).
 * Backfilled when another file with the same size is hashed (FR-FP-02).
 */
data class SizeBucketPendingEntry(
    val fileEntryId: Long,
    val rootId: Long,
    val uriOrPath: String,
    val displayName: String,
    val sizeBytes: Long,
    val mtimeNs: Long,
    val generation: Int,
) {
  fun toDiscoveredEntry(): DiscoveredEntry {
    val fromName = MediaTypeHint.fromFileName(displayName)
    val mediaHint =
        if (fromName != MediaTypeHint.OTHER) {
          fromName
        } else {
          MediaTypeHint.DOCUMENT
        }
    return DiscoveredEntry(
        contentUri = Uri.parse(uriOrPath),
        scanRootId = rootId,
        generation = generation,
        displayName = displayName,
        mediaTypeHint = mediaHint,
        sizeBytes = sizeBytes,
        mtimeNs = mtimeNs,
    )
  }
}
