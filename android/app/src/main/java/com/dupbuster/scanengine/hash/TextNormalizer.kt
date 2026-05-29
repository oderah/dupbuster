package com.dupbuster.scanengine.hash

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.text.Normalizer
import java.nio.charset.StandardCharsets

/**
 * Plain-text normalization for `TEXT_NFC_LF` (architecture §4.3 / FR-equiv-text-*).
 * UTF-8 decode → strip BOM → NFC → CRLF→LF; trailing whitespace is preserved.
 */
object TextNormalizer {

  private val UTF_8 = StandardCharsets.UTF_8
  private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

  sealed class Outcome {
    data class Ok(val normalizedUtf8: ByteArray) : Outcome()

    data object InvalidUtf8 : Outcome()
  }

  fun normalize(raw: ByteArray): Outcome {
    val withoutBom = stripUtf8Bom(raw)
    val text =
        try {
          UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(withoutBom))
              .toString()
        } catch (_: CharacterCodingException) {
          return Outcome.InvalidUtf8
        }

    val nfc = Normalizer.normalize(text, Normalizer.Form.NFC)
    val lfOnly = nfc.replace("\r\n", "\n")
    return Outcome.Ok(lfOnly.toByteArray(UTF_8))
  }

  private fun stripUtf8Bom(bytes: ByteArray): ByteArray {
    if (bytes.size < BOM.size) {
      return bytes
    }
    for (i in BOM.indices) {
      if (bytes[i] != BOM[i]) {
        return bytes
      }
    }
    return bytes.copyOfRange(BOM.size, bytes.size)
  }
}
