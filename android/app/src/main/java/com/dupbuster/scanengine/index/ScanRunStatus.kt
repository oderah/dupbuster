package com.dupbuster.scanengine.index

/** `scan_run.status` values — must match architecture §5.2 and ScanSessionController. */
object ScanRunStatus {
  const val RUNNING: String = "running"
  const val PAUSED: String = "paused"
  const val CANCELLING: String = "cancelling"
  const val CANCELLED: String = "cancelled"
  const val COMPLETE: String = "complete"
  const val ERROR: String = "error"

  val ACTIVE: Set<String> = setOf(RUNNING, PAUSED, CANCELLING)

  val RESUMABLE: Set<String> = setOf(RUNNING, PAUSED)
}
