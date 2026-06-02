package com.dupbuster.scanengine.delete

import android.net.Uri
import com.dupbuster.scanengine.security.ScanRootGrant

/**
 * Platform delete API surface (FR-AC-07). Production wiring lands in M3-04 (Android) / M3-05 (iOS).
 */
fun interface PlatformFileDeleter {
  /** @return true when the platform reports the file removed. */
  fun delete(uri: Uri, grant: ScanRootGrant): Boolean
}

/** Default until MediaStore / PHAsset delete is wired — coordinator counts failures, not catalog loss. */
class PendingPlatformFileDeleter : PlatformFileDeleter {
  override fun delete(uri: Uri, grant: ScanRootGrant): Boolean = false
}
