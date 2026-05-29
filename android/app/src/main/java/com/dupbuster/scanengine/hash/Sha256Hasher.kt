package com.dupbuster.scanengine.hash

import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

/** SHA-256 helpers for quick sample and streaming full hash (FR-FP-01). */
object Sha256Hasher {
  fun digestBytes(data: ByteArray): String = hexDigest(MessageDigest.getInstance("SHA-256").digest(data))

  fun digestQuickSample(firstChunk: ByteArray, lastChunk: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(firstChunk)
    digest.update(lastChunk)
    return hexDigest(digest.digest())
  }

  fun digestStream(
      input: InputStream,
      bufferSize: Int = HashConstants.HASH_READ_BUFFER_BYTES,
      deadlineMs: Long,
  ): StreamDigestOutcome {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(bufferSize)
    try {
      while (true) {
        if (System.currentTimeMillis() > deadlineMs) {
          return StreamDigestOutcome.Timeout
        }
        val read = input.read(buffer)
        if (read < 0) {
          break
        }
        if (read > 0) {
          digest.update(buffer, 0, read)
        }
      }
      return StreamDigestOutcome.Ok(hexDigest(digest.digest()))
    } catch (_: IOException) {
      return StreamDigestOutcome.IoFailure
    }
  }

  private fun hexDigest(bytes: ByteArray): String =
      bytes.joinToString(separator = "") { byte ->
        String.format(Locale.US, "%02x", byte)
      }
}

sealed class StreamDigestOutcome {
  data class Ok(val hexDigest: String) : StreamDigestOutcome()

  data object Timeout : StreamDigestOutcome()

  data object IoFailure : StreamDigestOutcome()
}
