package com.dupbuster.scanengine.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionFilterTest {

  @Test
  fun apply_androidStoragePath_isFullyRedacted() {
    val raw = "/storage/emulated/0/DCIM/test.jpg"
    val redacted = RedactionFilter.apply(raw)
    assertNotNull(redacted)
    assertFalse(redacted!!.contains(raw))
    assertTrue(redacted.contains(RedactionFilter.REDACTED_PLACEHOLDER))
  }

  @Test
  fun apply_contentUri_isRedacted() {
    val raw = "content://media/external/images/media/42"
    val redacted = RedactionFilter.apply(raw)!!
    assertFalse(redacted.contains("content://"))
    assertTrue(redacted.contains(RedactionFilter.REDACTED_PLACEHOLDER))
  }

  @Test
  fun apply_fileUri_isRedacted() {
    val raw = "file:///var/mobile/Media/DCIM/photo.heic"
    val redacted = RedactionFilter.apply(raw)!!
    assertFalse(redacted.contains("file://"))
  }

  @Test
  fun apply_phAssetUri_isRedacted() {
    val raw = "ph://ABCDEF-1234-5678"
    val redacted = RedactionFilter.apply(raw)!!
    assertFalse(redacted.contains("ph://"))
  }

  @Test
  fun apply_userHomePath_isRedacted() {
    val raw = "/Users/test/Pictures/vacation.png"
    val redacted = RedactionFilter.apply(raw)!!
    assertFalse(redacted.contains("/Users/test"))
  }

  @Test
  fun apply_windowsUserPath_isRedacted() {
    val raw = """C:\Users\test\Pictures\photo.jpg"""
    val redacted = RedactionFilter.apply(raw)!!
    assertFalse(redacted.contains("""C:\Users\test"""))
  }

  @Test
  fun apply_mediaPathSegment_isRedacted() {
    val raw = "failed to open folder/clip.mp4 for read"
    val redacted = RedactionFilter.apply(raw)!!
    assertFalse(redacted.contains("/clip.mp4"))
  }

  @Test
  fun applyToCrashPayload_sensitiveKeys_forceRedacted() {
    val payload =
        mapOf(
            "scan_run_id" to 7L,
            "uri_or_path" to "/storage/emulated/0/DCIM/test.jpg",
            "display_name" to "test.jpg",
            "nested" to
                mapOf(
                    "paths" to listOf("/sdcard/foo.jpg"),
                ),
        )
    val redacted = RedactionFilter.applyToCrashPayload(payload)
    assertEquals(7L, redacted["scan_run_id"])
    assertEquals(RedactionFilter.REDACTED_PLACEHOLDER, redacted["uri_or_path"])
    assertEquals(RedactionFilter.REDACTED_PLACEHOLDER, redacted["display_name"])
    @Suppress("UNCHECKED_CAST")
    val nested = redacted["nested"] as Map<String, Any?>
    assertEquals(RedactionFilter.REDACTED_PLACEHOLDER, nested["paths"])
  }

  @Test
  fun apply_closedSetReason_isUnchanged() {
    assertEquals("PERMISSION_DENIED", RedactionFilter.apply("PERMISSION_DENIED"))
  }
}
