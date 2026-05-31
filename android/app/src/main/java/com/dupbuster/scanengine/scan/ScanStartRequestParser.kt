package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.security.ScanRootMode
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap

object ScanStartRequestParser {

  fun parse(options: ReadableMap): ScanStartRequest {
    if (!options.hasKey("mode") || options.isNull("mode")) {
      throw IllegalArgumentException("startScan requires mode")
    }
    val modeValue = options.getString("mode")
        ?: throw IllegalArgumentException("startScan mode must be a string")
    val mode =
        ScanRootMode.fromBridgeValue(modeValue)
            ?: throw IllegalArgumentException("Unknown scan mode: $modeValue")

    val roots = parseRoots(options.getArray("roots"))

    val resumeScanRunId =
        if (options.hasKey("resumeScanRunId") && !options.isNull("resumeScanRunId")) {
          options.getDouble("resumeScanRunId").toLong()
        } else {
          null
        }

    return ScanStartRequest(mode = mode, roots = roots, resumeScanRunId = resumeScanRunId)
  }

  private fun parseRoots(array: ReadableArray?): List<ScanRootInput> {
    if (array == null) {
      return emptyList()
    }
    val roots = mutableListOf<ScanRootInput>()
    for (index in 0 until array.size()) {
      val rootMap =
          array.getMap(index)
              ?: throw IllegalArgumentException("roots[$index] must be an object")
      if (!rootMap.hasKey("uriGrant") || rootMap.isNull("uriGrant")) {
        throw IllegalArgumentException("roots[$index].uriGrant is required")
      }
      val uriGrant =
          rootMap.getString("uriGrant")
              ?: throw IllegalArgumentException("roots[$index].uriGrant must be a string")
      val scanRootId =
          if (rootMap.hasKey("scanRootId") && !rootMap.isNull("scanRootId")) {
            rootMap.getDouble("scanRootId").toLong()
          } else {
            null
          }
      roots.add(ScanRootInput(uriGrant = uriGrant, scanRootId = scanRootId))
    }
    return roots
  }
}
