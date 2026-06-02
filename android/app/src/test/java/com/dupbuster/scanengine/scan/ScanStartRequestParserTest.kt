package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.security.ScanRootMode
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanStartRequestParserTest {

  @Test
  fun parse_platformDiscoveryWithEmptyRoots() {
    val options =
        JavaOnlyMap().apply {
          putString("mode", "platform_discovery")
          putArray("roots", JavaOnlyArray())
        }

    val request = ScanStartRequestParser.parse(options)

    assertEquals(ScanRootMode.PLATFORM_DISCOVERY, request.mode)
    assertEquals(0, request.roots.size)
    assertNull(request.resumeScanRunId)
  }

  @Test
  fun parse_userSelectedWithRootGrant() {
    val roots =
        JavaOnlyArray().apply {
          pushMap(
              JavaOnlyMap().apply {
                putString("uriGrant", "content://test/tree/docs")
                putDouble("scanRootId", 7.0)
              },
          )
        }
    val options =
        JavaOnlyMap().apply {
          putString("mode", "user_selected")
          putArray("roots", roots)
        }

    val request = ScanStartRequestParser.parse(options)

    assertEquals(ScanRootMode.USER_SELECTED, request.mode)
    assertEquals(1, request.roots.size)
    assertEquals("content://test/tree/docs", request.roots[0].uriGrant)
    assertEquals(7L, request.roots[0].scanRootId)
  }

  @Test
  fun parse_largeFilesOptIn() {
    val options =
        JavaOnlyMap().apply {
          putString("mode", "platform_discovery")
          putArray("roots", JavaOnlyArray())
          putBoolean("largeFilesOptIn", true)
        }

    val request = ScanStartRequestParser.parse(options)

    assertEquals(true, request.largeFilesOptIn)
  }
}
