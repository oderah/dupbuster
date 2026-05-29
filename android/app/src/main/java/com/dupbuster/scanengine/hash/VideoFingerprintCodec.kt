package com.dupbuster.scanengine.hash

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Serializes frame dHashes for `fingerprint.frame_hashes_blob` (debug-only egress). */
object VideoFingerprintCodec {

  fun encodeFrameHashesBlob(frameHashes: LongArray): ByteArray {
    val buffer = ByteBuffer.allocate(frameHashes.size * Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    for (hash in frameHashes) {
      buffer.putLong(hash)
    }
    return buffer.array()
  }

  fun decodeFrameHashesBlob(blob: ByteArray): LongArray {
    require(blob.size % Long.SIZE_BYTES == 0) { "invalid frame_hashes_blob length" }
    val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
    val count = blob.size / Long.SIZE_BYTES
    return LongArray(count) { buffer.getLong() }
  }

  /** Stable hex digest stored in `fingerprint.hash_value` for exact-index lookups. */
  fun canonicalHashValue(frameHashes: LongArray): String =
      Sha256Hasher.digestBytes(encodeFrameHashesBlob(frameHashes))
}
