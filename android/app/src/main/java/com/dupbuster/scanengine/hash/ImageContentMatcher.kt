package com.dupbuster.scanengine.hash

/**
 * Hamming match for SAME_CONTENT_IMAGE (FR-FP-09).
 * Grouper clusters image rows via [matches] (fuzzy), not exact fingerprint id alone.
 */
object ImageContentMatcher {

  fun matches(
      left: Long,
      right: Long,
      hammingThreshold: Int = ImageConstants.HAMMING_THRESHOLD_DEFAULT,
  ): Boolean = DHash.hammingDistance(left, right) <= hammingThreshold
}
