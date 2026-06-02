package com.dupbuster.scanengine.hash

import com.dupbuster.scanengine.stat.StagedFile

/** Outcome of [HashPipeline.hash] for one staged file. */
sealed class HashResult {
  data class Success(val hashed: HashedFile) : HashResult()

  /**
   * Video two-path hash: [rawBytes] (`RAW_BYTES`) + [videoContent] (`VIDEO_CONTENT_V1`) in parallel (FR-FP-07).
   */
  data class VideoSuccess(
      val rawBytes: HashedFile,
      val videoContent: HashedFile,
  ) : HashResult()

  /**
   * RAW_BYTES succeeded; video content fingerprint failed (e.g. > 500 MB budget without opt-in).
   */
  data class VideoPartialSuccess(
      val rawBytes: HashedFile,
      val videoUnscannableReason: String,
  ) : HashResult()

  /** Image two-path hash: [rawBytes] (`RAW_BYTES`) + [imageContent] (`IMAGE_CONTENT_V1`) in parallel (FR-FP-09). */
  data class ImageSuccess(
      val rawBytes: HashedFile,
      val imageContent: HashedFile,
  ) : HashResult()

  data class ImagePartialSuccess(
      val rawBytes: HashedFile,
      val imageUnscannableReason: String,
  ) : HashResult()

  /** Unique size in catalog — no byte read (FR-FP-02). Caller may backfill when size collides. */
  data class SizeBucketSkipped(val staged: StagedFile) : HashResult()

  /** Symlink node indexed without following target (requirements §4). */
  data class SymlinkNode(val staged: StagedFile) : HashResult()

  data class Unscannable(val reason: String) : HashResult() {
    init {
      require(reason.isNotEmpty()) { "unscannable reason required" }
    }
  }
}
