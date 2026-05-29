package com.dupbuster.scanengine.hash

/**
 * Pre-bucket for video: registers [durationMs] before size-bucket elimination (M1-14 / FR-FP-02).
 * Video always proceeds to `RAW_BYTES` + `VIDEO_CONTENT_V1` regardless of disposition (FR-FP-07).
 */
interface DurationBucketIndex {
  fun register(durationMs: Long): DurationBucketDisposition
}

enum class DurationBucketDisposition {
  /** No other indexed video within the duration gate — still fingerprints. */
  UNIQUE_DURATION,
  /** Another indexed video may match under the duration gate. */
  HAS_GATE_CANDIDATE,
  /** Duration unknown — fingerprinting proceeds; gate matching deferred. */
  DURATION_UNKNOWN,
}

class InMemoryDurationBucketIndex : DurationBucketIndex {
  private val durations = mutableListOf<Long>()

  override fun register(durationMs: Long): DurationBucketDisposition {
    if (durationMs <= 0) {
      return DurationBucketDisposition.DURATION_UNKNOWN
    }
    val hasCandidate =
        durations.any { other ->
          VideoContentMatcher.passesDurationGate(durationMs, other)
        }
    durations.add(durationMs)
    return if (hasCandidate) {
      DurationBucketDisposition.HAS_GATE_CANDIDATE
    } else {
      DurationBucketDisposition.UNIQUE_DURATION
    }
  }

  fun registeredCount(): Int = durations.size
}
