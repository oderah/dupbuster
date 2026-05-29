package com.dupbuster.scanengine.stat

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/** Reads duration/dimensions via [MediaMetadataRetriever] (architecture §4.2 platform decoders). */
class AndroidVideoMetadataReader(
    context: Context,
) : VideoMetadataReader {

  private val contentResolver = context.contentResolver

  override fun readMetadata(uri: Uri): VideoMetadataReadOutcome {
    val retriever = MediaMetadataRetriever()
    return try {
      val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return VideoMetadataReadOutcome.Unavailable
      pfd.use {
        retriever.setDataSource(it.fileDescriptor)
        readFromRetriever(retriever)
      }
    } catch (_: Exception) {
      VideoMetadataReadOutcome.Unavailable
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {
        // ignore
      }
    }
  }

  private fun readFromRetriever(retriever: MediaMetadataRetriever): VideoMetadataReadOutcome {
    val durationText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val widthText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
    val heightText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
    val durationMs = durationText?.toLongOrNull() ?: return VideoMetadataReadOutcome.Unavailable
    val width = widthText?.toIntOrNull() ?: 0
    val height = heightText?.toIntOrNull() ?: 0
    return VideoMetadataReadOutcome.Ok(
        VideoMetadata(
            durationMs = durationMs.coerceAtLeast(0L),
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
        ),
    )
  }
}
