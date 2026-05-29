package com.dupbuster.scanengine.hash

import android.net.Uri
import java.io.InputStream

/** Byte reads for HashPipeline (injectable in unit tests). */
interface FileContentReader {
  fun openRead(uri: Uri): ContentOpenOutcome

  fun readRange(uri: Uri, offset: Long, length: Int): ByteArray?
}

sealed class ContentOpenOutcome {
  data class Ok(val stream: InputStream) : ContentOpenOutcome()

  data object IoFailure : ContentOpenOutcome()
}
