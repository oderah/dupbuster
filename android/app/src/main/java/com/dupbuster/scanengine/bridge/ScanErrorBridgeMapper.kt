package com.dupbuster.scanengine.bridge

import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReadableMap

/** Maps native scan errors to the RN `onScanError` payload (architecture §6 / bridge law). */
object ScanErrorBridgeMapper {

  val ALLOWED_BRIDGE_KEYS: Set<String> =
      setOf("fileEntryId", "unscannableReason", "scanRunId")

  fun toReadableMap(
      fileEntryId: Long,
      unscannableReason: String,
      scanRunId: Long? = null,
  ): ReadableMap {
    val map =
        JavaOnlyMap().apply {
          putDouble("fileEntryId", fileEntryId.toDouble())
          putString("unscannableReason", unscannableReason)
          if (scanRunId != null) {
            putDouble("scanRunId", scanRunId.toDouble())
          }
        }
    assertBridgeSafePayload(map)
    return map
  }

  fun assertBridgeSafePayload(map: ReadableMap) {
    val iterator = map.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      check(key in ALLOWED_BRIDGE_KEYS) { "Forbidden bridge field: $key" }
    }
  }
}
