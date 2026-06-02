package com.dupbuster.scanengine.hash

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

/**
 * Production [VideoFrameExtractor] via platform [MediaMetadataRetriever] (same stack as
 * [com.dupbuster.scanengine.stat.AndroidVideoMetadataReader]). Samples frames at timeline
 * positions, scales to ≤320×180, emits grayscale for dHash (FR-FP-07).
 */
class ContentResolverVideoFrameExtractor(
    private val context: Context,
) : VideoFrameExtractor {

  override fun extractFrames(
      uri: Uri,
      samplePositions: DoubleArray,
      deadlineMs: Long,
  ): VideoFrameExtractOutcome {
    if (System.currentTimeMillis() > deadlineMs) {
      return VideoFrameExtractOutcome.Timeout
    }
    val retriever = MediaMetadataRetriever()
    return try {
      val parcel =
          context.contentResolver.openFileDescriptor(uri, "r")
              ?: return VideoFrameExtractOutcome.DecodeFailed
      parcel.use {
        retriever.setDataSource(it.fileDescriptor)
        val durationMs =
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { value -> value > 0 }
                ?: return VideoFrameExtractOutcome.DecodeFailed
        val videoWidth =
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0
        val videoHeight =
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0
        val frames = mutableListOf<GrayFrame>()
        for (position in samplePositions) {
          if (System.currentTimeMillis() > deadlineMs) {
            return VideoFrameExtractOutcome.Timeout
          }
          val timeUs = (durationMs * 1_000.0 * position).toLong()
          val bitmap =
              retriever.getFrameAtTime(
                  timeUs,
                  MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
              ) ?: return VideoFrameExtractOutcome.DecodeFailed
          frames.add(bitmapToGrayFrame(scaleToFit(bitmap)))
        }
        if (frames.isEmpty()) {
          return VideoFrameExtractOutcome.DecodeFailed
        }
        VideoFrameExtractOutcome.Ok(
            frames = frames,
            durationMs = durationMs,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
        )
      }
    } catch (_: Exception) {
      VideoFrameExtractOutcome.DecodeFailed
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {
        // ignore
      }
    }
  }

  private fun scaleToFit(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val scale =
        minOf(
            VideoConstants.DHASH_MAX_WIDTH.toFloat() / width,
            VideoConstants.DHASH_MAX_HEIGHT.toFloat() / height,
            1f,
        )
    if (scale >= 1f) {
      return bitmap
    }
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    if (scaled !== bitmap) {
      bitmap.recycle()
    }
    return scaled
  }

  private fun bitmapToGrayFrame(bitmap: Bitmap): GrayFrame {
    val width = bitmap.width
    val height = bitmap.height
    val gray = ByteArray(width * height)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    bitmap.recycle()
    for (index in pixels.indices) {
      val pixel = pixels[index]
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      gray[index] = ((r * 77 + g * 150 + b * 29) shr 8).toByte()
    }
    return GrayFrame(width = width, height = height, pixels = gray)
  }
}
