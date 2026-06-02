package com.dupbuster.scanengine.hash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageContentMatcherTest {

  @Test
  fun matches_withinHammingThreshold() {
    val left = 0x0123_4567_89AB_CDEFL
    val close = left xor (1L shl 2)
    assertTrue(ImageContentMatcher.matches(left, close))
  }

  @Test
  fun matches_rejectsDistantHashes() {
    val left = 0x0123_4567_89AB_CDEFL
    val far = 0x7EDC_BA98_7654_3210L
    assertFalse(ImageContentMatcher.matches(left, far))
  }

  @Test
  fun matches_acceptsHammingNine_forRecompressedExports() {
    val left = 0x0123_4567_89AB_CDEFL
    var right = left
    repeat(9) { bit ->
      right = right xor (1L shl bit)
    }
    assertTrue(ImageContentMatcher.matches(left, right))
  }
}
