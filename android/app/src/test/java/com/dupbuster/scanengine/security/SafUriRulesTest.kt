package com.dupbuster.scanengine.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AC-security-uri-01 — crafted SAF docId with `..` must fail validation logic. */
class SafUriRulesTest {

  @Test
  fun documentIdHasTraversalSegment_detectsDotDotSegment() {
    assertTrue(SafUriRules.documentIdHasTraversalSegment("primary:Documents/../etc/passwd"))
    assertTrue(SafUriRules.documentIdHasTraversalSegment("primary:.."))
    assertTrue(SafUriRules.documentIdHasTraversalSegment(".."))
  }

  @Test
  fun documentIdHasTraversalSegment_allowsNormalPaths() {
    assertFalse(SafUriRules.documentIdHasTraversalSegment("primary:Documents/photos"))
    assertFalse(SafUriRules.documentIdHasTraversalSegment("primary:Documents"))
  }

  @Test
  fun isDocumentIdUnderTree_rejectsTraversal() {
    val tree = "primary:Documents"
    assertFalse(
        SafUriRules.isDocumentIdUnderTree(tree, "primary:Documents/../secret"),
    )
  }

  @Test
  fun isDocumentIdUnderTree_acceptsDescendant() {
    val tree = "primary:Documents"
    assertTrue(SafUriRules.isDocumentIdUnderTree(tree, "primary:Documents/photos/img.jpg"))
    assertTrue(SafUriRules.isDocumentIdUnderTree(tree, "primary:Documents"))
  }

  @Test
  fun isDocumentIdUnderTree_rejectsSiblingPrefixAttack() {
    val tree = "primary:Documents"
    assertFalse(
        SafUriRules.isDocumentIdUnderTree(tree, "primary:DocumentsExtra/file"),
    )
  }
}
