package com.dupbuster.scanengine.discovery

import android.net.Uri

/** One MediaStore row from a platform discovery query (testable without a live index). */
data class MediaStoreRow(
    val contentUri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val collectionKind: MediaStoreCollectionKind,
)
