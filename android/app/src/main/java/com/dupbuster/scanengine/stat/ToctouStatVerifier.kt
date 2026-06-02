package com.dupbuster.scanengine.stat

/**
 * Re-reads authoritative size/mtime after hash and compares to the StatStage baseline (FR-SI-01).
 */
class ToctouStatVerifier(
    private val fileStatReader: FileStatReader,
) {

  fun verifyBaseline(staged: StagedFile): ToctouVerifyOutcome {
    return when (val read = fileStatReader.readStat(staged.discovered.contentUri)) {
      FileStatReadOutcome.IoFailure -> ToctouVerifyOutcome.IoFailure
      is FileStatReadOutcome.Ok ->
          if (
              read.stat.sizeBytes == staged.sizeBytes && read.stat.mtimeNs == staged.mtimeNs
          ) {
            ToctouVerifyOutcome.Consistent
          } else {
            ToctouVerifyOutcome.Changed(read.stat)
          }
    }
  }

  fun applyFreshStat(staged: StagedFile, fresh: FileStat): StagedFile =
      staged.copy(
          sizeBytes = fresh.sizeBytes,
          mtimeNs = fresh.mtimeNs,
          inode = fresh.inode,
          deviceId = fresh.deviceId,
          isSymlink = fresh.isSymlink,
          durationMs = fresh.durationMs ?: staged.durationMs,
          videoWidth = fresh.videoWidth ?: staged.videoWidth,
          videoHeight = fresh.videoHeight ?: staged.videoHeight,
      )

  companion object {
    /** Caps re-queue loops when metadata keeps changing (e.g. active log file). */
    const val MAX_MISMATCH_RETRIES = 3
  }
}

sealed class ToctouVerifyOutcome {
  data object Consistent : ToctouVerifyOutcome()

  data class Changed(val freshStat: FileStat) : ToctouVerifyOutcome()

  data object IoFailure : ToctouVerifyOutcome()
}
