package com.dupbuster.scanengine.stat

import com.dupbuster.scanengine.security.UnscannableReason

/** Outcome of [StatStage.stat] for one discovered file. */
sealed class StatResult {
  data class Success(val staged: StagedFile) : StatResult()

  data class Unscannable(val reason: String) : StatResult() {
    init {
      require(reason.isNotEmpty()) { "unscannable reason required" }
    }

    companion object {
      fun permissionDenied(): Unscannable = Unscannable(UnscannableReason.PERMISSION_DENIED)
    }
  }
}
