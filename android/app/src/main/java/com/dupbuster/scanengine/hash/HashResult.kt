package com.dupbuster.scanengine.hash

import com.dupbuster.scanengine.stat.StagedFile

/** Outcome of [HashPipeline.hash] for one staged file. */
sealed class HashResult {
  data class Success(val hashed: HashedFile) : HashResult()

  /** Unique size in catalog — no byte read (FR-FP-02). Caller may backfill when size collides. */
  data class SizeBucketSkipped(val staged: StagedFile) : HashResult()

  /** Symlink node indexed without following target (requirements §4). */
  data class SymlinkNode(val staged: StagedFile) : HashResult()

  data class Unscannable(val reason: String) : HashResult() {
    init {
      require(reason.isNotEmpty()) { "unscannable reason required" }
    }
  }
}
