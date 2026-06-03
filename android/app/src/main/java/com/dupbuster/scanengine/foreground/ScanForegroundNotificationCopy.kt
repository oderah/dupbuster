package com.dupbuster.scanengine.foreground

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Frozen EN notification copy for Android FGS (requirements §9 / architecture §7.1).
 * Body template: `notification.scan.body` = "{percent}% · {filesProcessed} files".
 */
object ScanForegroundNotificationCopy {
  const val TITLE: String = "Scanning for duplicates"
  const val CHANNEL_NAME: String = "Duplicate scan"

  fun formatBody(filesProcessed: Int, filesTotalKnown: Int?): String {
    val percent =
        if (filesTotalKnown == null || filesTotalKnown <= 0) {
          0
        } else {
          val ratio = filesProcessed.toDouble() / filesTotalKnown.toDouble()
          min(100, max(0, (ratio * 100.0).roundToInt()))
        }
    return "$percent% · $filesProcessed files"
  }
}
