package com.dupbuster.scanengine.delete

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/** Platform ContentResolver / MediaStore delete operations (test-injectable). */
interface ContentDeleteGateway {
  fun deleteSafDocument(uri: Uri): Boolean

  fun deleteMediaStoreUris(uris: List<Uri>): Boolean
}

class DefaultContentDeleteGateway(
    private val context: Context,
    private val deleteConfirmationLauncher: DeleteConfirmationLauncher?,
) : ContentDeleteGateway {

  private val contentResolver
    get() = context.contentResolver

  override fun deleteSafDocument(uri: Uri): Boolean {
    if (uri.authority !in SAF_AUTHORITIES) {
      return false
    }
    return try {
      DocumentsContract.deleteDocument(contentResolver, uri)
    } catch (_: Exception) {
      false
    }
  }

  override fun deleteMediaStoreUris(uris: List<Uri>): Boolean {
    if (uris.isEmpty()) {
      return true
    }
    return when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
          deleteMediaStoreBatchApi30(uris, deleteConfirmationLauncher)
      else -> deleteMediaStoreBatchLegacy(uris)
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun deleteMediaStoreBatchApi30(
      uris: List<Uri>,
      launcher: DeleteConfirmationLauncher?,
  ): Boolean {
    if (launcher == null) {
      return false
    }
    return try {
      val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
      launcher.launchForResult(pendingIntent.intentSender)
    } catch (_: Exception) {
      false
    }
  }

  private fun deleteMediaStoreBatchLegacy(uris: List<Uri>): Boolean {
    var allSucceeded = true
    for (uri in uris) {
      if (!deleteMediaStoreUriLegacy(uri)) {
        allSucceeded = false
      }
    }
    return allSucceeded
  }

  private fun deleteMediaStoreUriLegacy(uri: Uri): Boolean {
    return try {
      contentResolver.delete(uri, null, null) > 0
    } catch (exception: RecoverableSecurityException) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val launcher = deleteConfirmationLauncher ?: return false
        launcher.launchForResult(exception.userAction.actionIntent.intentSender)
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }
  }

  companion object {
    private val SAF_AUTHORITIES =
        setOf(
            "com.android.externalstorage.documents",
            "com.android.providers.downloads.documents",
        )
  }
}
