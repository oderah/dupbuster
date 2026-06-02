package com.dupbuster.scanengine.hash

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.dupbuster.scanengine.scan.ScanOpenFileRegistry
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** Read-only content access via `ContentResolver.openFileDescriptor` (architecture §6). */
class ContentResolverFileContentReader(
    context: Context,
    private val openFileRegistry: ScanOpenFileRegistry? = null,
) : FileContentReader {

  private val contentResolver = context.contentResolver

  override fun openRead(uri: Uri): ContentOpenOutcome {
    val pfd =
        try {
          contentResolver.openFileDescriptor(uri, "r")
        } catch (_: IOException) {
          return ContentOpenOutcome.IoFailure
        } catch (_: SecurityException) {
          return ContentOpenOutcome.IoFailure
        } ?: return ContentOpenOutcome.IoFailure

    val release = openFileRegistry?.register(pfd)
    return try {
      ContentOpenOutcome.Ok(
          RegistryTrackingInputStream(
              input = FileInputStream(pfd.fileDescriptor),
              onClose = { release?.close() ?: pfd.close() },
          ),
      )
    } catch (_: IOException) {
      release?.close() ?: pfd.close()
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
        } catch (_: SecurityException) {
          return null
        } ?: return null

    val release = openFileRegistry?.register(pfd)
    return try {
      readRangeFromPfd(pfd, offset, length)
    } finally {
      release?.close() ?: pfd.close()
    }
  }

  private class RegistryTrackingInputStream(
      input: InputStream,
      private val onClose: () -> Unit,
  ) : FilterInputStream(input) {
    override fun close() {
      super.close()
      onClose()
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
