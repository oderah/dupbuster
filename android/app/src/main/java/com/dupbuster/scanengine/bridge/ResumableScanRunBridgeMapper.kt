package com.dupbuster.scanengine.bridge

import com.dupbuster.scanengine.index.ResumableScanRun
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap

/** Maps [ResumableScanRun] to bridge-safe metadata (no paths/hashes). */
object ResumableScanRunBridgeMapper {
  private val ALLOWED_KEYS = setOf("scanRunId", "lastProcessedId", "status")

  fun toReadableMap(run: ResumableScanRun): ReadableMap =
      Arguments.createMap().apply {
        putDouble("scanRunId", run.scanRunId.toDouble())
        putDouble("lastProcessedId", run.lastProcessedId.toDouble())
        putString("status", run.status)
      }

  fun assertBridgeSafePayload(payload: ReadableMap) {
    val iterator = payload.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      require(key in ALLOWED_KEYS) { "Unexpected resumable scan bridge key: $key" }
    }
  }
}
