package com.dupbuster.scanengine.bridge

import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

/** Maps [ScanProgressSnapshot] to the RN `onScanProgress` payload (architecture §6.5). */
object ScanProgressBridgeMapper {

  val ALLOWED_BRIDGE_KEYS: Set<String> =
      setOf(
          "filesProcessed",
          "filesTotalKnown",
          "groupsFound",
          "reclaimableBytesEst",
          "phase",
          "contentKind",
      )

  fun toReadableMap(snapshot: ScanProgressSnapshot): ReadableMap =
      toWritableMap(snapshot)

  fun toWritableMap(snapshot: ScanProgressSnapshot): WritableMap {
    val map =
        JavaOnlyMap().apply {
          putDouble("filesProcessed", snapshot.filesProcessed.toDouble())
          if (snapshot.filesTotalKnown != null) {
            putDouble("filesTotalKnown", snapshot.filesTotalKnown.toDouble())
          } else {
            putNull("filesTotalKnown")
          }
          putDouble("groupsFound", snapshot.groupsFound.toDouble())
          putDouble("reclaimableBytesEst", snapshot.reclaimableBytesEst.toDouble())
          putString("phase", snapshot.phase)
          val contentKind = ScanProgressContentKindPolicy.bridgeContentKind(snapshot.phase, snapshot.contentKind)
          if (contentKind != null) {
            require(ScanProgressContentKindPolicy.isAllowedBridgeValue(contentKind)) {
              "Invalid contentKind: $contentKind"
            }
            putString("contentKind", contentKind)
          }
        }
    return map
  }

  /** Test guard: bridge payloads must never expand beyond the progress contract. */
  fun assertBridgeSafePayload(map: ReadableMap) {
    val iterator = map.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      check(key in ALLOWED_BRIDGE_KEYS) { "Forbidden bridge field: $key" }
    }
    if (map.hasKey("contentKind") && !map.isNull("contentKind")) {
      val value = map.getString("contentKind")
      check(value != null && ScanProgressContentKindPolicy.isAllowedBridgeValue(value)) {
        "Invalid contentKind on bridge: $value"
      }
    }
  }
}
