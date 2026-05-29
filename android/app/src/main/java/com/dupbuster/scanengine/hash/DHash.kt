package com.dupbuster.scanengine.hash

import kotlin.math.max
import kotlin.math.min

/**
 * 64-bit difference hash on grayscale frames (FR-FP-07).
 * Downscales to ≤ [VideoConstants.DHASH_MAX_WIDTH]×[VideoConstants.DHASH_MAX_HEIGHT] then 9×8 compare grid.
 */
object DHash {

  fun compute(grayPixels: ByteArray, width: Int, height: Int): Long {
    val bounded = downscaleGray(grayPixels, width, height, VideoConstants.DHASH_MAX_WIDTH, VideoConstants.DHASH_MAX_HEIGHT)
    val grid =
        downscaleGray(
            bounded.pixels,
            bounded.width,
            bounded.height,
            VideoConstants.DHASH_COMPARE_WIDTH,
            VideoConstants.DHASH_COMPARE_HEIGHT,
        )
    return differenceHash(grid.pixels, grid.width, grid.height)
  }

  fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

  private fun differenceHash(gray: ByteArray, width: Int, height: Int): Long {
    require(width == VideoConstants.DHASH_COMPARE_WIDTH)
    require(height == VideoConstants.DHASH_COMPARE_HEIGHT)
    var hash = 0L
    var bitIndex = 0
    for (row in 0 until height) {
      val rowOffset = row * width
      for (col in 0 until width - 1) {
        val left = gray[rowOffset + col].toInt() and 0xFF
        val right = gray[rowOffset + col + 1].toInt() and 0xFF
        if (left < right) {
          hash = hash or (1L shl bitIndex)
        }
        bitIndex++
      }
    }
    return hash
  }

  private data class GrayImage(val pixels: ByteArray, val width: Int, val height: Int)

  private fun downscaleGray(
      pixels: ByteArray,
      width: Int,
      height: Int,
      targetWidth: Int,
      targetHeight: Int,
  ): GrayImage {
    if (width <= 0 || height <= 0) {
      return GrayImage(ByteArray(targetWidth * targetHeight), targetWidth, targetHeight)
    }
    val out = ByteArray(targetWidth * targetHeight)
    for (y in 0 until targetHeight) {
      val srcY = y * height / targetHeight
      for (x in 0 until targetWidth) {
        val srcX = x * width / targetWidth
        out[y * targetWidth + x] = pixels[srcY * width + srcX]
      }
    }
    return GrayImage(out, targetWidth, targetHeight)
  }
}
