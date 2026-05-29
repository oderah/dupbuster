package com.dupbuster.scanengine.bridge

/** Optional progress subcopy hint (M1-16 bridge field); throttle passes through when set. */
object ScanProgressContentKind {
  const val NONE: String = "none"
  const val VIDEO_CONTENT: String = "video_content"
}
