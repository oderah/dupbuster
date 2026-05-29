package com.dupbuster.scanengine.hash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFingerprintCodecTest {

  @Test
  fun encodeAndDecode_roundTripsFrameHashes() {
    val hashes = longArrayOf(1L, 2L, 3L, 4L, 5L)
    val blob = VideoFingerprintCodec.encodeFrameHashesBlob(hashes)
    assertArrayEquals(hashes, VideoFingerprintCodec.decodeFrameHashesBlob(blob))
  }

  @Test
  fun canonicalHashValue_isStableSha256Hex() {
    val hashes = longArrayOf(0xABCDL, 0x1234L)
    val first = VideoFingerprintCodec.canonicalHashValue(hashes)
    val second = VideoFingerprintCodec.canonicalHashValue(hashes)
    assertEquals(first, second)
    assertEquals(64, first.length)
  }
}
