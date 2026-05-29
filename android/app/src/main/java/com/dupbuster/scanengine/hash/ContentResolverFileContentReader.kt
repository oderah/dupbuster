package com.dupbuster.scanengine.hash

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.IOException

/** Read-only content access via `ContentResolver.openFileDescriptor` (architecture §6). */
class ContentResolverFileContentReader(
    context: Context,
) : FileContentReader {

  private val contentResolver = context.contentResolver

  override fun openRead(uri: Uri): ContentOpenOutcome {
    val pfd =
        try {
          contentResolver.openFileDescriptor(uri, "r")
        } catch (_: IOException) {
          return ContentOpenOutcome.IoFailure
        } ?: return ContentOpenOutcome.IoFailure

    return try {
      ContentOpenOutcome.Ok(FileInputStream(pfd.fileDescriptor))
    } catch (_: IOException) {
      pfd.close()
      ContentOpenOutcome.IoFailure
    }
  }

  override fun readRange(uri: Uri, offset: Long, length: Int): ByteArray? {
    if (length <= 0) {
      return ByteArray(0)
    }
    val pfd =
        try {
          contentResolver.openFileDescriptor(uri, "r")
        } catch (_: IOException) {
          return null
        } ?: return null

  return pfd.use { parcel ->
      readRangeFromPfd(parcel, offset, length)
    }
  }

  private fun readRangeFromPfd(pfd: ParcelFileDescriptor, offset: Long, length: Int): ByteArray? {
    return try {
      FileInputStream(pfd.fileDescriptor).use { input ->
        val skipped = input.skip(offset)
        if (skipped < offset) {
          return null
        }
        val buffer = ByteArray(length)
        var filled = 0
        while (filled < length) {
          val read = input.read(buffer, filled, length - filled)
          if (read < 0) {
            break
          }
          filled += read
        }
        if (filled == length) {
          buffer
        } else {
          buffer.copyOf(filled)
        }
      }
    } catch (_: IOException) {
      null
    }
  }
}
