package com.dupbuster.scanengine.bridge

/** Scan phase wire values — must match [NativeScanEngine.ts] and ScanSessionController. */
object ScanPhase {
  const val IDLE: String = "idle"
  const val DISCOVERING: String = "discovering"
  const val HASHING: String = "hashing"
  const val GROUPING: String = "grouping"
  const val COMPLETE: String = "complete"
  const val PAUSED: String = "paused"
  const val ERROR: String = "error"
  const val CANCELLING: String = "cancelling"
  const val CANCELLED: String = "cancelled"
}
