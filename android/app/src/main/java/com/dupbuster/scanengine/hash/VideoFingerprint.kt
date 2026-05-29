package com.dupbuster.scanengine.hash

/** Grayscale frame from [VideoFrameExtractor] (width × height bytes, row-major). */
data class GrayFrame(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
) {
  init {
    require(width > 0 && height > 0) { "frame dimensions required" }
    require(pixels.size == width * height) { "pixel buffer size mismatch" }
  }
}

/** Successful VIDEO_CONTENT_V1 output (FR-FP-07). */
data class VideoFingerprint(
    val frameHashes: LongArray,
    val durationMs: Long,
    val videoWidth: Int,
    val videoHeight: Int,
) {
  val hashValue: String
    get() = VideoFingerprintCodec.canonicalHashValue(frameHashes)

  val frameHashesBlob: ByteArray
    get() = VideoFingerprintCodec.encodeFrameHashesBlob(frameHashes)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as VideoFingerprint
    return frameHashes.contentEquals(other.frameHashes) &&
        durationMs == other.durationMs &&
        videoWidth == other.videoWidth &&
        videoHeight == other.videoHeight
  }

  override fun hashCode(): Int = frameHashes.contentHashCode()
}
