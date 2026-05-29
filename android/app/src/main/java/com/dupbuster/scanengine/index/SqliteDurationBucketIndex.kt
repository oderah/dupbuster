package com.dupbuster.scanengine.index

import com.dupbuster.scanengine.hash.DurationBucketDisposition
import com.dupbuster.scanengine.hash.DurationBucketIndex

/** SQLite-backed duration pre-bucket for production video scans (M1-14). */
class SqliteDurationBucketIndex(private val indexWriter: IndexWriter) : DurationBucketIndex {

  override fun register(durationMs: Long): DurationBucketDisposition {
    if (durationMs <= 0) {
      return DurationBucketDisposition.DURATION_UNKNOWN
    }
    val existing = indexWriter.countVideosWithinDurationGate(durationMs)
    return if (existing > 0) {
      DurationBucketDisposition.HAS_GATE_CANDIDATE
    } else {
      DurationBucketDisposition.UNIQUE_DURATION
    }
  }
}
