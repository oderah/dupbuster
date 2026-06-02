package com.dupbuster.scanengine.index

/** `duplicate_group.match_kind` values (architecture §5.2 / requirements §11). */
object MatchKind {
  const val EXACT_BYTES: String = "EXACT_BYTES"
  const val SAME_CONTENT_VIDEO: String = "SAME_CONTENT_VIDEO"
  const val SAME_CONTENT_IMAGE: String = "SAME_CONTENT_IMAGE"

  const val CONFIDENCE_EXACT: Double = 1.0
  const val CONFIDENCE_VIDEO_CONTENT: Double = 0.95
  const val CONFIDENCE_IMAGE_CONTENT: Double = 0.95
}
