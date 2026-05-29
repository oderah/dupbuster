package com.dupbuster.scanengine.stat

import android.net.Uri

/** Low-level stat via platform I/O (injectable for unit tests). */
interface FileStatReader {
  fun readStat(uri: Uri): FileStatReadOutcome
}

sealed class FileStatReadOutcome {
  data class Ok(val stat: FileStat) : FileStatReadOutcome()

  /** Stat/open failed (permission revoked, provider error, missing document). */
  data object IoFailure : FileStatReadOutcome()
}
