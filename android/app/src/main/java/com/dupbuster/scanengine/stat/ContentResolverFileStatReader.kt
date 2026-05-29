package com.dupbuster.scanengine.stat

import android.content.Context
import android.net.Uri
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.IOException

/**
 * Stat via `ContentResolver.openFileDescriptor` + `Os.fstat` (architecture §6 Android).
 * Symlinks are not followed at hash open time; when detectable, `isSymlink` is set.
 */
class ContentResolverFileStatReader(
    context: Context,
) : FileStatReader {

  private val contentResolver = context.contentResolver

  override fun readStat(uri: Uri): FileStatReadOutcome {
    val pfd =
        try {
          contentResolver.openFileDescriptor(uri, "r")
        } catch (_: IOException) {
          return FileStatReadOutcome.IoFailure
        } ?: return FileStatReadOutcome.IoFailure

    pfd.use { parcel ->
      val structStat =
          try {
            Os.fstat(parcel.fileDescriptor)
          } catch (_: ErrnoException) {
            return FileStatReadOutcome.IoFailure
          }

      val mtimeNs = mtimeNsFromStructStat(structStat)
      val isSymlink = isSymlinkAtOpenFd(parcel.fd)

      return FileStatReadOutcome.Ok(
          FileStat(
              sizeBytes = structStat.st_size.coerceAtLeast(0L),
              mtimeNs = mtimeNs,
              inode = structStat.st_ino,
              deviceId = structStat.st_dev,
              isSymlink = isSymlink,
          ),
      )
    }
  }

  private fun mtimeNsFromStructStat(structStat: android.system.StructStat): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val timespec = structStat.st_mtim
      timespec.tv_sec * 1_000_000_000L + timespec.tv_nsec
    } else {
      @Suppress("DEPRECATION")
      structStat.st_mtime * 1_000_000_000L
    }
  }

  /**
   * Best-effort symlink detection without following at open time. Content providers may
   * still resolve links; full symlink fixtures are validated in M1-17.
   */
  private fun isSymlinkAtOpenFd(fd: Int): Boolean {
    val fdPath = "/proc/self/fd/$fd"
    return try {
      val target = Os.readlink(fdPath) ?: return false
      val linkStat = Os.lstat(target)
      (linkStat.st_mode and OsConstants.S_IFMT) == OsConstants.S_IFLNK
    } catch (_: ErrnoException) {
      false
    }
  }
}
