package com.dupbuster.scanengine.discovery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

interface MediaStoreDiscoveryQuery {
  fun queryCollection(kind: MediaStoreCollectionKind): List<MediaStoreRow>
}

class ContentResolverMediaStoreDiscoveryQuery(
    context: Context,
) : MediaStoreDiscoveryQuery {

  private val resolver: ContentResolver = context.contentResolver

  override fun queryCollection(kind: MediaStoreCollectionKind): List<MediaStoreRow> {
    if (kind == MediaStoreCollectionKind.DOWNLOAD && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return emptyList()
    }

    val spec = collectionSpec(kind) ?: return emptyList()
    val rows = mutableListOf<MediaStoreRow>()
    resolver.query(spec.contentUri, spec.projection, null, null, null)?.use { cursor ->
      val idIdx = cursor.getColumnIndexOrThrow(spec.idColumn)
      val nameIdx = cursor.getColumnIndexOrThrow(spec.displayNameColumn)
      val mimeIdx = cursor.getColumnIndexOrThrow(spec.mimeTypeColumn)
      val sizeIdx = cursor.getColumnIndexOrThrow(spec.sizeColumn)
      val modifiedIdx = cursor.getColumnIndexOrThrow(spec.modifiedColumn)
      while (cursor.moveToNext()) {
        val id = cursor.getLong(idIdx)
        val displayName = cursor.getString(nameIdx) ?: "media-$id"
        val mimeType = cursor.getString(mimeIdx)
        val sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L)
        // MediaStore DATE_MODIFIED is seconds since epoch (not milliseconds).
        val lastModifiedMs = cursor.getLong(modifiedIdx) * 1000L
        val contentUri = ContentUris.withAppendedId(spec.contentUri, id)
        rows.add(
            MediaStoreRow(
                contentUri = contentUri,
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                lastModifiedMs = lastModifiedMs,
                collectionKind = kind,
            ),
        )
      }
    }
    return rows
  }

  private data class CollectionSpec(
      val contentUri: Uri,
      val idColumn: String,
      val displayNameColumn: String,
      val mimeTypeColumn: String,
      val sizeColumn: String,
      val modifiedColumn: String,
  ) {
    val projection: Array<String> =
        arrayOf(idColumn, displayNameColumn, mimeTypeColumn, sizeColumn, modifiedColumn)
  }

  private fun collectionSpec(kind: MediaStoreCollectionKind): CollectionSpec? {
    return when (kind) {
      MediaStoreCollectionKind.IMAGE ->
          CollectionSpec(
              contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
              idColumn = MediaStore.Images.Media._ID,
              displayNameColumn = MediaStore.Images.Media.DISPLAY_NAME,
              mimeTypeColumn = MediaStore.Images.Media.MIME_TYPE,
              sizeColumn = MediaStore.Images.Media.SIZE,
              modifiedColumn = MediaStore.Images.Media.DATE_MODIFIED,
          )
      MediaStoreCollectionKind.VIDEO ->
          CollectionSpec(
              contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
              idColumn = MediaStore.Video.Media._ID,
              displayNameColumn = MediaStore.Video.Media.DISPLAY_NAME,
              mimeTypeColumn = MediaStore.Video.Media.MIME_TYPE,
              sizeColumn = MediaStore.Video.Media.SIZE,
              modifiedColumn = MediaStore.Video.Media.DATE_MODIFIED,
          )
      MediaStoreCollectionKind.AUDIO ->
          CollectionSpec(
              contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
              idColumn = MediaStore.Audio.Media._ID,
              displayNameColumn = MediaStore.Audio.Media.DISPLAY_NAME,
              mimeTypeColumn = MediaStore.Audio.Media.MIME_TYPE,
              sizeColumn = MediaStore.Audio.Media.SIZE,
              modifiedColumn = MediaStore.Audio.Media.DATE_MODIFIED,
          )
      MediaStoreCollectionKind.DOWNLOAD ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CollectionSpec(
                contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                idColumn = MediaStore.Downloads._ID,
                displayNameColumn = MediaStore.Downloads.DISPLAY_NAME,
                mimeTypeColumn = MediaStore.Downloads.MIME_TYPE,
                sizeColumn = MediaStore.Downloads.SIZE,
                modifiedColumn = MediaStore.Downloads.DATE_MODIFIED,
            )
          } else {
            null
          }
    }
  }
}
