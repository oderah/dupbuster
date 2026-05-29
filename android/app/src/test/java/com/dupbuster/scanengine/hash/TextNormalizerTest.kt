package com.dupbuster.scanengine.hash

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

  @Test
  fun normalize_crlfAndLfOnly_produceSameBytes() {
    val crlf = "line1\r\nline2\n".toByteArray(StandardCharsets.UTF_8)
    val lfOnly = "line1\nline2\n".toByteArray(StandardCharsets.UTF_8)

    val crlfNorm = normalizeOk(crlf)
    val lfNorm = normalizeOk(lfOnly)

    assertArrayEquals(lfNorm, crlfNorm)
  }

  @Test
  fun normalize_bomStripped_matchesNoBom() {
    val withBom =
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "same".toByteArray(StandardCharsets.UTF_8)
    val withoutBom = "same".toByteArray(StandardCharsets.UTF_8)

    assertArrayEquals(normalizeOk(withoutBom), normalizeOk(withBom))
  }

  @Test
  fun normalize_nfdAndNfc_produceSameBytes() {
    val nfc = "caf\u00E9".toByteArray(StandardCharsets.UTF_8)
    val nfd = "cafe\u0301".toByteArray(StandardCharsets.UTF_8)

    assertArrayEquals(normalizeOk(nfc), normalizeOk(nfd))
  }

  @Test
  fun normalize_trailingWhitespaceDiffers() {
    val withSpaces = "hello  ".toByteArray(StandardCharsets.UTF_8)
    val withoutSpaces = "hello".toByteArray(StandardCharsets.UTF_8)

    val spaced = normalizeOk(withSpaces)
    val plain = normalizeOk(withoutSpaces)

    assertEquals("hello  ", String(spaced, StandardCharsets.UTF_8))
    assertTrue(!spaced.contentEquals(plain))
  }

  @Test
  fun normalize_invalidUtf8_returnsInvalid() {
    val invalid = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    assertTrue(TextNormalizer.normalize(invalid) is TextNormalizer.Outcome.InvalidUtf8)
  }

  private fun normalizeOk(raw: ByteArray): ByteArray {
    val outcome = TextNormalizer.normalize(raw)
    assertTrue(outcome is TextNormalizer.Outcome.Ok)
    return (outcome as TextNormalizer.Outcome.Ok).normalizedUtf8
  }
}
