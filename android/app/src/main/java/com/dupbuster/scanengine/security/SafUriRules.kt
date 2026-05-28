package com.dupbuster.scanengine.security

/**
 * Pure SAF document-id rules (unit-testable without Robolectric).
 * Platform I/O uses [android.provider.DocumentsContract] — no string path concatenation.
 */
internal object SafUriRules {
  /** AC-security-uri-01: reject crafted docIds that escape the tree via `..`. */
  fun documentIdHasTraversalSegment(documentId: String): Boolean {
    if (documentId.isEmpty()) {
      return false
    }
    return documentId.split('/').any { segment ->
      segment == ".." || segment.contains("..")
    }
  }

  /**
   * Grant boundary: [documentId] must be the tree root or a direct descendant in document-id space.
   */
  fun isDocumentIdUnderTree(treeDocumentId: String, documentId: String): Boolean {
    if (documentIdHasTraversalSegment(documentId) || documentIdHasTraversalSegment(treeDocumentId)) {
      return false
    }
    if (documentId == treeDocumentId) {
      return true
    }
    val prefix = "$treeDocumentId/"
    return documentId.startsWith(prefix)
  }

  fun extractDocumentIdFromUriPath(uriPath: String?): String? {
    if (uriPath.isNullOrEmpty()) {
      return null
    }
    val treeMarker = "/tree/"
    val documentMarker = "/document/"
    val treeIdx = uriPath.indexOf(treeMarker)
    if (treeIdx >= 0) {
      return uriPath.substring(treeIdx + treeMarker.length)
    }
    val docIdx = uriPath.indexOf(documentMarker)
    if (docIdx >= 0) {
      return uriPath.substring(docIdx + documentMarker.length)
    }
    return null
  }
}
