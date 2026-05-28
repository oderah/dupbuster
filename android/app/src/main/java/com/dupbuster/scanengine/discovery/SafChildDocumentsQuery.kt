package com.dupbuster.scanengine.discovery

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

interface SafChildDocumentsQuery {
  fun queryChildren(treeUri: Uri, parentDocumentId: String): List<SafChildRow>
}

class ContentResolverSafChildDocumentsQuery(
    private val context: Context,
) : SafChildDocumentsQuery {

  private val resolver: ContentResolver = context.contentResolver

  override fun queryChildren(treeUri: Uri, parentDocumentId: String): List<SafChildRow> {
    val childrenUri =
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
    val projection =
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    val rows = mutableListOf<SafChildRow>()
    resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
      val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
      val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
      val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
      val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
      val modifiedIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
      while (cursor.moveToNext()) {
        val documentId = cursor.getString(idIdx) ?: continue
        val displayName = cursor.getString(nameIdx) ?: documentId
        val mimeType = cursor.getString(mimeIdx)
        val sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L)
        val lastModifiedMs = cursor.getLong(modifiedIdx)
        val isDirectory =
            mimeType == DocumentsContract.Document.MIME_TYPE_DIR ||
                mimeType == "vnd.android.document/directory"
        rows.add(
            SafChildRow(
                documentId = documentId,
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                lastModifiedMs = lastModifiedMs,
                isDirectory = isDirectory,
            ),
        )
      }
    }
    return rows
  }
}
