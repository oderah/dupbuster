package com.dupbuster.scanengine.hash

import android.net.Uri

/** Decodes a still image to grayscale pixels for [ImageFingerprinter]. */
fun interface ImageBitmapExtractor {
  sealed class Outcome {
    data class Ok(val frame: GrayFrame) : Outcome()

    data object DecodeFailed : Outcome()
  }

  fun decode(uri: Uri, deadlineMs: Long): Outcome
}
