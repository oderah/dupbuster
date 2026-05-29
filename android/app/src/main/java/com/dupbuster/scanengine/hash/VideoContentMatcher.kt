package com.dupbuster.scanengine.hash

/**
 * Duration gate + Hamming frame-pair matching for SAME_CONTENT_VIDEO (FR-FP-07).
 * Grouper uses this for fuzzy video grouping (M1-18 fixture suite).
 */
object VideoContentMatcher {

  fun passesDurationGate(durationA: Long, durationB: Long): Boolean {
    if (durationA <= 0 || durationB <= 0) {
      return false
    }
    val minDuration = minOf(durationA, durationB)
    val delta = kotlin.math.abs(durationA - durationB)
    if (delta >= VideoConstants.DURATION_GATE_MIN_MS) {
      return false
    }
    return delta <= (minDuration * VideoConstants.DURATION_GATE_RATIO).toLong()
  }

  /**
   * Returns true when ≥ [VideoConstants.MIN_MATCHING_FRAME_PAIRS] frame pairs are within
   * [hammingThreshold] (ship default ≤ 8).
   */
  fun framePairsMatch(
      framesA: LongArray,
      framesB: LongArray,
      hammingThreshold: Int = VideoConstants.HAMMING_THRESHOLD_DEFAULT,
  ): Boolean {
    if (framesA.isEmpty() || framesB.isEmpty()) {
      return false
    }
    val pairCount = minOf(framesA.size, framesB.size)
    var matchingPairs = 0
    for (index in 0 until pairCount) {
      if (DHash.hammingDistance(framesA[index], framesB[index]) <= hammingThreshold) {
        matchingPairs++
      }
    }
    return matchingPairs >= VideoConstants.MIN_MATCHING_FRAME_PAIRS
  }

  fun contentMatches(
      left: VideoFingerprint,
      right: VideoFingerprint,
      hammingThreshold: Int = VideoConstants.HAMMING_THRESHOLD_DEFAULT,
  ): Boolean {
    if (!passesDurationGate(left.durationMs, right.durationMs)) {
      return false
    }
    return framePairsMatch(left.frameHashes, right.frameHashes, hammingThreshold)
  }
}
