package com.dupbuster.scanengine.stat

import android.content.Context
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.UriProvenance
import com.dupbuster.scanengine.security.UriValidationResult
import com.dupbuster.scanengine.security.UriValidator

/**
 * Mandatory stat after discovery and UriValidator gate (architecture §3.2 StatStage).
 * Re-reads size/mtime/inode/device_id for TOCTOU baseline (FR-SI-01).
 */
class StatStage(
    context: Context,
    private val uriValidator: UriValidator = UriValidator(context),
    private val fileStatReader: FileStatReader = ContentResolverFileStatReader(context),
) {

  fun stat(
      entry: DiscoveredEntry,
      grant: ScanRootGrant,
      provenance: UriProvenance = UriProvenance.DISCOVERY,
  ): StatResult {
    when (
        uriValidator.validate(
            entry.contentUri,
            grant,
            provenance,
        )
    ) {
      is UriValidationResult.Denied -> return StatResult.Unscannable.permissionDenied()
      UriValidationResult.Allowed -> Unit
    }

    return when (val read = fileStatReader.readStat(entry.contentUri)) {
      FileStatReadOutcome.IoFailure -> StatResult.Unscannable.permissionDenied()
      is FileStatReadOutcome.Ok ->
          StatResult.Success(
              StagedFile(
                  discovered = entry,
                  sizeBytes = read.stat.sizeBytes,
                  mtimeNs = read.stat.mtimeNs,
                  inode = read.stat.inode,
                  deviceId = read.stat.deviceId,
                  isSymlink = read.stat.isSymlink,
                  mediaTypeHint = entry.mediaTypeHint,
              ),
          )
    }
  }
}
