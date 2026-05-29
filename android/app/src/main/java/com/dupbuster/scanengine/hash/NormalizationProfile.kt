package com.dupbuster.scanengine.hash

/** `fingerprint.normalization_profile` values (architecture §5). */
object NormalizationProfile {
  const val RAW_BYTES: String = "RAW_BYTES"
  const val TEXT_NFC_LF: String = "TEXT_NFC_LF"
  const val EMPTY: String = "EMPTY:0"
  /** Video content fingerprint (M1-13+); grouper maps to [MatchKind.SAME_CONTENT_VIDEO]. */
  const val VIDEO_CONTENT_V1: String = "VIDEO_CONTENT_V1"
}
