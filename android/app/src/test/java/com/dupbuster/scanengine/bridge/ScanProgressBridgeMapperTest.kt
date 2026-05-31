package com.dupbuster.scanengine.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanProgressBridgeMapperTest {

  @Test
  fun toReadableMap_hashingVideoContent_includesContentKind_acIntegrityProgress02() {
    val snapshot =
        ScanProgressSnapshot(
            filesProcessed = 42,
            filesTotalKnown = 100,
            groupsFound = 0,
            reclaimableBytesEst = 0L,
            phase = ScanPhase.HASHING,
            contentKind = ScanProgressContentKind.VIDEO_CONTENT,
        )

    val map = ScanProgressBridgeMapper.toReadableMap(snapshot)

    ScanProgressBridgeMapper.assertBridgeSafePayload(map)
    assertEquals(42.0, map.getDouble("filesProcessed"), 0.0)
    assertEquals(100.0, map.getDouble("filesTotalKnown"), 0.0)
    assertEquals(ScanPhase.HASHING, map.getString("phase"))
    assertTrue(map.hasKey("contentKind"))
    assertEquals(ScanProgressContentKind.VIDEO_CONTENT, map.getString("contentKind"))
    var keyCount = 0
    val keys = map.keySetIterator()
    while (keys.hasNextKey()) {
      keyCount++
      keys.nextKey()
    }
    assertEquals(6, keyCount)
  }

  @Test
  fun toReadableMap_discovering_omitsContentKind() {
    val snapshot =
        ScanProgressSnapshot(
            filesProcessed = 1,
            filesTotalKnown = null,
            groupsFound = 0,
            reclaimableBytesEst = 0L,
            phase = ScanPhase.DISCOVERING,
            contentKind = ScanProgressContentKind.VIDEO_CONTENT,
        )

    val map = ScanProgressBridgeMapper.toReadableMap(snapshot)

    ScanProgressBridgeMapper.assertBridgeSafePayload(map)
    assertFalse(map.hasKey("contentKind"))
  }

  @Test
  fun toReadableMap_hashingNone_includesNone() {
    val snapshot =
        progress(phase = ScanPhase.HASHING, contentKind = ScanProgressContentKind.NONE)

    val map = ScanProgressBridgeMapper.toReadableMap(snapshot)

    assertEquals(ScanProgressContentKind.NONE, map.getString("contentKind"))
    ScanProgressBridgeMapper.assertBridgeSafePayload(map)
  }

  @Test
  fun assertBridgeSafePayload_rejectsForbiddenKeys() {
    val map =
        com.facebook.react.bridge.JavaOnlyMap().apply {
          putDouble("filesProcessed", 1.0)
          putString("uri_or_path", "content://secret")
        }

    try {
      ScanProgressBridgeMapper.assertBridgeSafePayload(map)
      org.junit.Assert.fail("Expected forbidden bridge field rejection")
    } catch (_: IllegalStateException) {
      // expected
    }
  }

  private fun progress(phase: String, contentKind: String?): ScanProgressSnapshot =
      ScanProgressSnapshot(
          filesProcessed = 1,
          filesTotalKnown = 10,
          groupsFound = 0,
          reclaimableBytesEst = 0L,
          phase = phase,
          contentKind = contentKind,
      )
}
