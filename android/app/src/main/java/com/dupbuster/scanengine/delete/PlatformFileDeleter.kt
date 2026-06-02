package com.dupbuster.scanengine.delete

import com.dupbuster.scanengine.index.FileEntryDeleteTarget

/**
 * Platform delete API surface (FR-AC-07). Production wiring: [AndroidPlatformFileDeleter] (M3-04)
 * / iOS PHAsset (M3-05).
 */
fun interface PlatformFileDeleter {
  /** @return [file_entry_id] values removed on disk. */
  fun deleteTargets(targets: List<FileEntryDeleteTarget>): Set<Long>
}

/** Test / headless fallback — coordinator counts failures, catalog unchanged. */
class PendingPlatformFileDeleter : PlatformFileDeleter {
  override fun deleteTargets(targets: List<FileEntryDeleteTarget>): Set<Long> = emptySet()
}
