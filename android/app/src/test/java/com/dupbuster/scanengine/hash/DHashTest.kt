package com.dupbuster.scanengine.hash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DHashTest {

  @Test
  fun compute_sameInput_producesStableHash() {
    val pixels = gradientFrame(width = 16, height = 16)
    val first = DHash.compute(pixels, 16, 16)
    val second = DHash.compute(pixels, 16, 16)
    assertEquals(first, second)
  }

  @Test
  fun compute_differentInput_producesDifferentHash() {
    val left = gradientFrame(width = 16, height = 16)
    val right = ByteArray(16 * 16) { 255.toByte() }
    assertTrue(DHash.compute(left, 16, 16) != DHash.compute(right, 16, 16))
  }

  @Test
  fun hammingDistance_countsBitDifferences() {
    assertEquals(0, DHash.hammingDistance(0L, 0L))
    assertEquals(1, DHash.hammingDistance(0L, 1L))
    assertEquals(64, DHash.hammingDistance(0L, -1L))
  }

  private fun gradientFrame(width: Int, height: Int): ByteArray {
    val pixels = ByteArray(width * height)
    for (y in 0 until height) {
      for (x in 0 until width) {
        pixels[y * width + x] = ((x + y) * 8).toByte()
      }
    }
    return pixels
  }
}
