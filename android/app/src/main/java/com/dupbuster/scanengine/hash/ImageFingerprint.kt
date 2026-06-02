package com.dupbuster.scanengine.hash

/** Successful IMAGE_CONTENT_V1 output (FR-FP-09). */
data class ImageFingerprint(
    val dHash: Long,
) {
  val hashValue: String
    get() = VideoFingerprintCodec.canonicalHashValue(longArrayOf(dHash))

  val frameHashesBlob: ByteArray
    get() = VideoFingerprintCodec.encodeFrameHashesBlob(longArrayOf(dHash))
}
