package com.dupbuster.scanengine.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeHintTest {

  @Test
  fun fromMimeType_mapsVideoAndText() {
    assertEquals(MediaTypeHint.VIDEO, MediaTypeHint.fromMimeType("video/mp4"))
    assertEquals(MediaTypeHint.TEXT, MediaTypeHint.fromMimeType("text/plain"))
    assertEquals(MediaTypeHint.IMAGE, MediaTypeHint.fromMimeType("image/jpeg"))
  }

  @Test
  fun fromFileName_mapsCommonExtensions() {
    assertEquals(MediaTypeHint.VIDEO, MediaTypeHint.fromFileName("clip.MP4"))
    assertEquals(MediaTypeHint.DOCUMENT, MediaTypeHint.fromFileName("report.pdf"))
    assertEquals(MediaTypeHint.OTHER, MediaTypeHint.fromFileName("noextension"))
  }
}
