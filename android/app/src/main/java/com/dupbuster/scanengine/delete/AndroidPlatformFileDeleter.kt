package com.dupbuster.scanengine.delete

import android.content.Context
import android.net.Uri
import com.dupbuster.scanengine.index.FileEntryDeleteTarget
import com.dupbuster.scanengine.security.ScanRootMode

/**
 * Production Android delete (M3-04): SAF [DocumentsContract.deleteDocument] and MediaStore
 * [MediaStore.createDeleteRequest] on API 30+ with a single batched user confirmation.
 */
class AndroidPlatformFileDeleter(
    context: Context,
    deleteConfirmationLauncher: DeleteConfirmationLauncher?,
    private val contentDeleteGateway: ContentDeleteGateway =
        DefaultContentDeleteGateway(context, deleteConfirmationLauncher),
) : PlatformFileDeleter {

  override fun deleteTargets(targets: List<FileEntryDeleteTarget>): Set<Long> {
    if (targets.isEmpty()) {
      return emptySet()
    }

    val deletedIds = mutableSetOf<Long>()
    val mediaStoreBatch = mutableListOf<FileEntryDeleteTarget>()

    for (target in targets) {
      val uri = Uri.parse(target.uriOrPath)
      if (shouldUseMediaStoreDelete(uri, target)) {
        mediaStoreBatch.add(target)
      } else if (contentDeleteGateway.deleteSafDocument(uri)) {
        deletedIds.add(target.fileEntryId)
      }
    }

    if (mediaStoreBatch.isNotEmpty()) {
      val uris = mediaStoreBatch.map { Uri.parse(it.uriOrPath) }
      if (contentDeleteGateway.deleteMediaStoreUris(uris)) {
        deletedIds.addAll(mediaStoreBatch.map { it.fileEntryId })
      }
    }

    return deletedIds
  }

  internal fun shouldUseMediaStoreDelete(uri: Uri, target: FileEntryDeleteTarget): Boolean {
    if (target.grant.mode != ScanRootMode.PLATFORM_DISCOVERY) {
      return false
    }
    return isMediaStoreAuthority(uri.authority)
  }

  private fun isMediaStoreAuthority(authority: String?): Boolean {
    return authority != null && authority in MEDIA_STORE_AUTHORITIES
  }

  companion object {
    private val MEDIA_STORE_AUTHORITIES =
        setOf(
            "media",
            "com.android.providers.media.documents",
        )
  }
}
