package com.dupbuster.scanengine.discovery

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.dupbuster.scanengine.security.SafUriRules
import com.dupbuster.scanengine.security.UriProvenance
import com.dupbuster.scanengine.security.UriValidationResult
import com.dupbuster.scanengine.security.UriValidator

fun interface DiscoveryEntryConsumer {
  fun onEntry(entry: DiscoveredEntry)
}

/**
 * Enumerates files for scan mode A (SAF / DocumentPicker user-selected root).
 * Mode B (MediaStore / PHAsset) is implemented in M1-05.
 */
class DiscoveryEmitter(
    context: Context,
    private val childQuery: SafChildDocumentsQuery = ContentResolverSafChildDocumentsQuery(context),
    private val uriValidator: UriValidator = UriValidator(context),
) {

  /**
   * Walks the granted SAF tree breadth-first, yielding every 32 emitted files
   * (architecture platform rules — batch size 32).
   */
  fun emitModeA(
      request: DiscoveryRequest,
      consumer: DiscoveryEntryConsumer,
      isCancelled: () -> Boolean = { false },
  ): DiscoveryResult {
    val treeUri = request.grant.uriGrant
    if (!DocumentsContract.isTreeUri(treeUri)) {
      return DiscoveryResult(
          entriesEmitted = 0,
          entriesDenied = 0,
          directoriesVisited = 0,
          cancelled = false,
      )
    }

    val rootDocumentId =
        runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return DiscoveryResult(0, 0, 0, false)

    var emitted = 0
    var denied = 0
    var directoriesVisited = 0
    var batchCount = 0

    val queue = ArrayDeque<String>()
    queue.add(rootDocumentId)

    while (queue.isNotEmpty()) {
      if (isCancelled()) {
        return DiscoveryResult(emitted, denied, directoriesVisited, cancelled = true)
      }

      val parentId = queue.removeFirst()
      directoriesVisited++

      val children =
          runCatching { childQuery.queryChildren(treeUri, parentId) }.getOrElse { emptyList() }

      for (child in children) {
        if (isCancelled()) {
          return DiscoveryResult(emitted, denied, directoriesVisited, cancelled = true)
        }

        if (child.isDirectory) {
          queue.add(child.documentId)
          continue
        }

        if (SafUriRules.documentIdHasTraversalSegment(child.documentId)) {
          denied++
          continue
        }

        val documentUri =
            DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId)

        when (
            uriValidator.validate(
                documentUri,
                request.grant,
                UriProvenance.DISCOVERY,
            )
        ) {
          is UriValidationResult.Denied -> {
            denied++
            continue
          }
          UriValidationResult.Allowed -> Unit
        }

        val mediaHint =
            MediaTypeHint.fromMimeType(child.mimeType)
                .let { hint ->
                  if (hint == MediaTypeHint.OTHER) {
                    MediaTypeHint.fromFileName(child.displayName)
                  } else {
                    hint
                  }
                }

        consumer.onEntry(
            DiscoveredEntry(
                contentUri = documentUri,
                scanRootId = request.scanRootId,
                generation = request.generation,
                displayName = child.displayName,
                mediaTypeHint = mediaHint,
                sizeBytes = child.sizeBytes,
                mtimeNs = child.lastModifiedMs * 1_000_000L,
            ),
        )
        emitted++
        batchCount++
        if (batchCount >= BATCH_SIZE) {
          batchCount = 0
          Thread.yield()
        }
      }
    }

    return DiscoveryResult(emitted, denied, directoriesVisited, cancelled = false)
  }

  companion object {
    const val BATCH_SIZE: Int = 32
  }
}
