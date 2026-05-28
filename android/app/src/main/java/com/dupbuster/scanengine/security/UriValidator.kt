package com.dupbuster.scanengine.security

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Mandatory gate before stat/open/hash (FR-SE-01, architecture §8.1).
 *
 * - API-only [Uri] resolution (no manual path concatenation)
 * - Authority allowlist + grant boundary under active [ScanRootGrant]
 * - Fail-closed → [UnscannableReason.PERMISSION_DENIED]
 * - Symlinks are not followed at open time (enforced in StatStage / hash FD open)
 */
class UriValidator(
    private val context: Context,
) {

  fun validate(
      candidate: Uri,
      grant: ScanRootGrant,
      provenance: UriProvenance,
  ): UriValidationResult {
    if (provenance == UriProvenance.USER_SUPPLIED) {
      return UriValidationResult.Denied()
    }

    if (!isAllowedScheme(candidate)) {
      return UriValidationResult.Denied()
    }

    val authority = candidate.authority ?: return UriValidationResult.Denied()
    if (!isAuthorityAllowed(authority, grant.mode)) {
      return UriValidationResult.Denied()
    }

    if (hasTraversalInSafUri(candidate)) {
      return UriValidationResult.Denied()
    }

    if (!isWithinGrantBoundary(candidate, grant)) {
      return UriValidationResult.Denied()
    }

    return UriValidationResult.Allowed
  }

  private fun isAllowedScheme(uri: Uri): Boolean {
    return when (uri.scheme?.lowercase()) {
      "content" -> true
      else -> false
    }
  }

  private fun isAuthorityAllowed(authority: String, mode: ScanRootMode): Boolean {
    return when (mode) {
      ScanRootMode.USER_SELECTED -> authority in SAF_AUTHORITIES
      ScanRootMode.PLATFORM_DISCOVERY ->
          authority in PLATFORM_DISCOVERY_AUTHORITIES
    }
  }

  private fun hasTraversalInSafUri(uri: Uri): Boolean {
    if (uri.authority !in SAF_AUTHORITIES) {
      return false
    }
    val documentId =
        runCatching {
              when {
                DocumentsContract.isTreeUri(uri) ->
                    DocumentsContract.getTreeDocumentId(uri)
                DocumentsContract.isDocumentUri(context, uri) ->
                    DocumentsContract.getDocumentId(uri)
                else -> SafUriRules.extractDocumentIdFromUriPath(uri.path)
              }
            }
            .getOrNull()
    if (documentId.isNullOrEmpty()) {
      return false
    }
    return SafUriRules.documentIdHasTraversalSegment(documentId)
  }

  private fun isWithinGrantBoundary(candidate: Uri, grant: ScanRootGrant): Boolean {
    return when (grant.mode) {
      ScanRootMode.USER_SELECTED -> isUnderSafTreeGrant(candidate, grant.uriGrant)
      ScanRootMode.PLATFORM_DISCOVERY ->
          isPlatformDiscoveryCandidate(candidate, grant.uriGrant)
    }
  }

  private fun isUnderSafTreeGrant(candidate: Uri, treeGrant: Uri): Boolean {
    val treeDocumentId = resolveDocumentId(treeGrant, preferTree = true) ?: return false
    val candidateDocumentId = resolveDocumentId(candidate, preferTree = false) ?: return false
    return SafUriRules.isDocumentIdUnderTree(treeDocumentId, candidateDocumentId)
  }

  private fun resolveDocumentId(uri: Uri, preferTree: Boolean): String? {
    return runCatching {
          when {
            preferTree && DocumentsContract.isTreeUri(uri) ->
                DocumentsContract.getTreeDocumentId(uri)
            DocumentsContract.isDocumentUri(context, uri) ->
                DocumentsContract.getDocumentId(uri)
            DocumentsContract.isTreeUri(uri) -> DocumentsContract.getTreeDocumentId(uri)
            else -> SafUriRules.extractDocumentIdFromUriPath(uri.path)
          }
        }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() }
  }

  private fun isPlatformDiscoveryCandidate(candidate: Uri, grantMarker: Uri): Boolean {
    val authority = candidate.authority ?: return false
    if (authority in MEDIA_STORE_AUTHORITIES) {
      return DocumentsContract.isDocumentUri(context, candidate) ||
          candidate.lastPathSegment != null
    }
    if (authority in SAF_AUTHORITIES) {
      return isUnderSafTreeGrant(candidate, grantMarker)
    }
    return false
  }

  companion object {
    private val SAF_AUTHORITIES =
        setOf(
            "com.android.externalstorage.documents",
            "com.android.providers.downloads.documents",
        )

    private val MEDIA_STORE_AUTHORITIES =
        setOf(
            "media",
            "com.android.providers.media.documents",
        )

    private val PLATFORM_DISCOVERY_AUTHORITIES =
        SAF_AUTHORITIES + MEDIA_STORE_AUTHORITIES
  }
}
