package com.dupbuster.scanengine.hash

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.system.Os
import android.system.OsConstants

/** Production image decode via [BitmapFactory] + [openFileDescriptor]. */
class ContentResolverImageBitmapExtractor(
    private val context: Context,
) : ImageBitmapExtractor {

  override fun decode(uri: Uri, deadlineMs: Long): ImageBitmapExtractor.Outcome {
    if (System.currentTimeMillis() > deadlineMs) {
      return ImageBitmapExtractor.Outcome.DecodeFailed
    }
    return try {
      val orientation = readExifOrientation(uri)
      context.contentResolver.openFileDescriptor(uri, "r")?.use { parcel ->
        val bounds =
            BitmapFactory.Options().apply {
              inJustDecodeBounds = true
            }
        BitmapFactory.decodeFileDescriptor(parcel.fileDescriptor, null, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
          return ImageBitmapExtractor.Outcome.DecodeFailed
        }
        Os.lseek(parcel.fileDescriptor, 0, OsConstants.SEEK_SET)
        val sampleSize =
            calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                VideoConstants.DHASH_MAX_WIDTH,
                VideoConstants.DHASH_MAX_HEIGHT,
            )
        val decodeOptions =
            BitmapFactory.Options().apply {
              inSampleSize = sampleSize
              inPreferredConfig = Bitmap.Config.RGB_565
            }
        var bitmap =
            BitmapFactory.decodeFileDescriptor(parcel.fileDescriptor, null, decodeOptions)
                ?: return ImageBitmapExtractor.Outcome.DecodeFailed
        bitmap = applyExifOrientation(bitmap, orientation)
        bitmap = scaleToFit(bitmap, VideoConstants.DHASH_MAX_WIDTH, VideoConstants.DHASH_MAX_HEIGHT)
        ImageBitmapExtractor.Outcome.Ok(bitmapToGrayFrame(bitmap))
      } ?: ImageBitmapExtractor.Outcome.DecodeFailed
    } catch (_: Exception) {
      ImageBitmapExtractor.Outcome.DecodeFailed
    }
  }

  private fun readExifOrientation(uri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
      } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix =
        Matrix().apply {
          when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
              postRotate(90f)
              postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
              postRotate(270f)
              postScale(-1f, 1f)
            }
            else -> Unit
          }
        }
    if (matrix.isIdentity) {
      return bitmap
    }
    val rotated =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
  }

  private fun scaleToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height, 1f)
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

  private fun calculateInSampleSize(
      width: Int,
      height: Int,
      maxWidth: Int,
      maxHeight: Int,
  ): Int {
    var sampleSize = 1
    if (height > maxHeight || width > maxWidth) {
      var halfHeight = height / 2
      var halfWidth = width / 2
      while (halfHeight / sampleSize >= maxHeight && halfWidth / sampleSize >= maxWidth) {
        sampleSize *= 2
      }
    }
    return sampleSize.coerceAtLeast(1)
  }
}
