package com.dupbuster.scanengine.foreground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ScanForegroundNotificationCopyTest {

  @Test
  fun formatBody_matchesFrozenTokenTemplate() {
    assertEquals("50% · 25 files", ScanForegroundNotificationCopy.formatBody(25, 50))
  }

  @Test
  fun formatBody_unknownTotalUsesZeroPercent() {
    assertEquals("0% · 3 files", ScanForegroundNotificationCopy.formatBody(3, null))
  }

  @Test
  fun frozenCopy_doesNotContainDeleteOrRemove() {
    val forbidden = setOf("delete", "remove", "Delete", "Remove")
    val fields =
        listOf(
            ScanForegroundNotificationCopy.TITLE,
            ScanForegroundNotificationCopy.CHANNEL_NAME,
            ScanForegroundNotificationCopy.formatBody(1, 10),
        )
    for (value in fields) {
      for (word in forbidden) {
        assertFalse("$value must not contain $word", value.contains(word, ignoreCase = true))
      }
    }
  }

  @Test
  fun channelId_isStable() {
    assertEquals("com.dupbuster.scan.foreground.v1", ScanForegroundConstants.CHANNEL_ID)
  }
}
