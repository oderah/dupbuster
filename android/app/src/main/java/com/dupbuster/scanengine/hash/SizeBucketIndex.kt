package com.dupbuster.scanengine.hash

import com.dupbuster.scanengine.discovery.MediaTypeHint

/**
 * Tracks per-size occurrence counts for size-bucket elimination (FR-FP-02).
 * Replaced by SQLite-backed counts when IndexWriter lands (M1-09).
 */
interface SizeBucketIndex {
  /**
   * Registers [sizeBytes] and returns whether this file should be read for hashing.
   * Zero-byte and video rows always require a read.
   */
  fun register(
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint,
      isEmpty: Boolean,
  ): SizeBucketDisposition
}

enum class SizeBucketDisposition {
  /** Only occurrence of this size so far — skip byte reads. */
  UNIQUE_SKIP,
  /** Second+ occurrence or exempt type — proceed with hash pipeline. */
  NEEDS_HASH,
}

class InMemorySizeBucketIndex : SizeBucketIndex {
  private val counts = mutableMapOf<Long, Int>()

  override fun register(
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint,
      isEmpty: Boolean,
  ): SizeBucketDisposition {
    if (isEmpty || mediaTypeHint == MediaTypeHint.VIDEO) {
      return SizeBucketDisposition.NEEDS_HASH
    }
    val next = (counts[sizeBytes] ?: 0) + 1
    counts[sizeBytes] = next
    return if (next == 1) {
      SizeBucketDisposition.UNIQUE_SKIP
    } else {
      SizeBucketDisposition.NEEDS_HASH
    }
  }

  fun count(sizeBytes: Long): Int = counts[sizeBytes] ?: 0
}
