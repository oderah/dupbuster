package com.dupbuster.scanengine.stat

import android.net.Uri

/** Video container metadata for duration gate / fingerprint sampling (M1-13). */
interface VideoMetadataReader {
  fun readMetadata(uri: Uri): VideoMetadataReadOutcome
}

data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
)

sealed class VideoMetadataReadOutcome {
  data class Ok(val metadata: VideoMetadata) : VideoMetadataReadOutcome()

  data object Unavailable : VideoMetadataReadOutcome()
}
