package com.dupbuster.scanengine

import com.facebook.proguard.annotations.DoNotStrip
import com.dupbuster.scanengine.bridge.CatalogSnapshotBridgeMapper
import com.dupbuster.scanengine.bridge.ResumableScanRunBridgeMapper
import com.dupbuster.scanengine.security.RedactionFilter
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.CatalogReader
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.delete.DeleteCoordinatorFactory
import com.dupbuster.scanengine.delete.DeleteDuplicatesCommandParser
import com.dupbuster.scanengine.scan.ScanOrchestrator
import com.dupbuster.scanengine.scan.ScanOrchestratorFactory
import com.dupbuster.scanengine.scan.ScanStartRequestParser
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule

/**
 * ScanEngine Turbo Module (M1-02). Pipeline orchestrator wired M1-19.
 * Bridge contract: [src/native/NativeScanEngine.ts]
 */
@DoNotStrip
@ReactModule(name = ScanEngineModule.NAME)
class ScanEngineModule(reactContext: ReactApplicationContext) :
    NativeScanEngineSpec(reactContext) {

  private fun emitProgressOnModuleThread(payload: ReadableMap) {
    if (!reactApplicationContext.hasActiveReactInstance()) {
      return
    }
    reactApplicationContext.runOnNativeModulesQueueThread {
      if (reactApplicationContext.hasActiveReactInstance()) {
        emitOnScanProgress(toEventEmitterMap(payload))
      }
    }
  }

  private fun emitErrorOnModuleThread(payload: ReadableMap) {
    if (!reactApplicationContext.hasActiveReactInstance()) {
      return
    }
    reactApplicationContext.runOnNativeModulesQueueThread {
      if (reactApplicationContext.hasActiveReactInstance()) {
        emitOnScanError(toEventEmitterMap(payload))
      }
    }
  }

  /**
   * TurboModule / JSI needs [WritableNativeMap] for nested catalog payloads; flat [JavaOnlyMap]
   * is insufficient for `getCatalogSnapshot` (duplicateGroups arrives empty on JS otherwise).
   */
  private fun toWritableMap(payload: ReadableMap): WritableMap {
    val map = Arguments.createMap()
    val iterator = payload.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      when (payload.getType(key)) {
        ReadableType.Null -> map.putNull(key)
        ReadableType.Boolean -> map.putBoolean(key, payload.getBoolean(key))
        ReadableType.Number -> map.putDouble(key, payload.getDouble(key))
        ReadableType.String -> map.putString(key, payload.getString(key))
        ReadableType.Map -> {
          val nested = payload.getMap(key)
          if (nested != null) {
            map.putMap(key, toWritableMap(nested))
          } else {
            map.putNull(key)
          }
        }
        ReadableType.Array -> {
          val nested = payload.getArray(key)
          if (nested != null) {
            map.putArray(key, toWritableArray(nested))
          } else {
            map.putNull(key)
          }
        }
        else -> Unit
      }
    }
    return map
  }

  private fun toWritableArray(payload: ReadableArray): WritableArray {
    val array = Arguments.createArray()
    for (index in 0 until payload.size()) {
      when (payload.getType(index)) {
        ReadableType.Null -> array.pushNull()
        ReadableType.Boolean -> array.pushBoolean(payload.getBoolean(index))
        ReadableType.Number -> array.pushDouble(payload.getDouble(index))
        ReadableType.String -> array.pushString(payload.getString(index))
        ReadableType.Map -> {
          val nested = payload.getMap(index)
          if (nested != null) {
            array.pushMap(toWritableMap(nested))
          } else {
            array.pushNull()
          }
        }
        ReadableType.Array -> {
          val nested = payload.getArray(index)
          if (nested != null) {
            array.pushArray(toWritableArray(nested))
          } else {
            array.pushNull()
          }
        }
        else -> Unit
      }
    }
    return array
  }

  /**
   * Codegen EventEmitter callbacks require [WritableNativeMap]; [JavaOnlyMap] is fine for
   * [Promise.resolve] but crashes `emitOnScanProgress` / `emitOnScanError`.
   */
  private fun toEventEmitterMap(payload: ReadableMap): WritableMap {
    val nativeMap = Arguments.createMap()
    val iterator = payload.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      when (payload.getType(key)) {
        ReadableType.Null -> nativeMap.putNull(key)
        ReadableType.Boolean -> nativeMap.putBoolean(key, payload.getBoolean(key))
        ReadableType.Number -> nativeMap.putDouble(key, payload.getDouble(key))
        ReadableType.String ->
            nativeMap.putString(
                key,
                RedactionFilter.apply(payload.getString(key)) ?: payload.getString(key),
            )
        else ->
            throw IllegalArgumentException(
                "Unsupported TurboModule event field type for key: $key",
            )
      }
    }
    return nativeMap
  }

  private val orchestrator: ScanOrchestrator by lazy {
    ScanOrchestratorFactory.create(
        context = reactApplicationContext,
        emitProgress = ::emitProgressOnModuleThread,
        emitError = ::emitErrorOnModuleThread,
    )
  }

  private val deleteCoordinator by lazy {
    DeleteCoordinatorFactory.create(reactApplicationContext)
  }

  override fun getName(): String = NAME

  @DoNotStrip
  override fun startScan(options: ReadableMap, promise: Promise) {
    try {
      val request = ScanStartRequestParser.parse(options)
      val scanRunId = orchestrator.startScan(request)
      val result =
          Arguments.createMap().apply {
            putDouble("scanRunId", scanRunId.toDouble())
          }
      promise.resolve(result)
    } catch (error: UnsupportedOperationException) {
      promise.reject(CODE_NOT_IMPLEMENTED, error.message, error)
    } catch (error: Exception) {
      promise.reject(CODE_SCAN_START_FAILED, error.message, error)
    }
  }

  @DoNotStrip
  override fun pauseScan(scanRunId: Double, promise: Promise) {
    try {
      orchestrator.pauseScan(scanRunId.toLong())
      promise.resolve(null)
    } catch (error: Exception) {
      promise.reject(CODE_SCAN_CONTROL_FAILED, error.message, error)
    }
  }

  @DoNotStrip
  override fun resumeScan(scanRunId: Double, promise: Promise) {
    try {
      orchestrator.resumeScan(scanRunId.toLong())
      promise.resolve(null)
    } catch (error: Exception) {
      promise.reject(CODE_SCAN_CONTROL_FAILED, error.message, error)
    }
  }

  @DoNotStrip
  override fun cancelScan(scanRunId: Double, promise: Promise) {
    try {
      orchestrator.cancelScan(scanRunId.toLong())
      promise.resolve(null)
    } catch (error: Exception) {
      promise.reject(CODE_SCAN_CONTROL_FAILED, error.message, error)
    }
  }

  @DoNotStrip
  override fun deleteDuplicates(command: ReadableMap, promise: Promise) {
    val parsed =
        try {
          DeleteDuplicatesCommandParser.parse(command)
        } catch (error: IllegalArgumentException) {
          promise.reject(CODE_DELETE_INVALID, error.message, error)
          return
        }

    deleteCoordinator.deleteDuplicates(parsed) { result ->
      reactApplicationContext.runOnNativeModulesQueueThread {
        if (!reactApplicationContext.hasActiveReactInstance()) {
          return@runOnNativeModulesQueueThread
        }
        val payload =
            Arguments.createMap().apply {
              putDouble("deletedCount", result.deletedCount.toDouble())
              putDouble("failedCount", result.failedCount.toDouble())
            }
        promise.resolve(payload)
      }
    }
  }

  @DoNotStrip
  override fun getResumableScanRun(promise: Promise) {
    try {
      val resumable = orchestrator.getResumableScanRun()
      if (resumable == null) {
        promise.resolve(null)
        return
      }
      val payload = ResumableScanRunBridgeMapper.toReadableMap(resumable)
      ResumableScanRunBridgeMapper.assertBridgeSafePayload(payload)
      promise.resolve(payload)
    } catch (error: Exception) {
      promise.reject(CODE_RESUMABLE_SCAN_FAILED, error.message, error)
    }
  }

  @DoNotStrip
  override fun abandonScanForRestart(scanRunId: Double, promise: Promise) {
    try {
      orchestrator.abandonScanForRestart(scanRunId.toLong())
      promise.resolve(null)
    } catch (error: Exception) {
      promise.reject(CODE_SCAN_CONTROL_FAILED, error.message, error)
    }
  }

  @DoNotStrip
  override fun getCatalogMeta(promise: Promise) {
    val catalogMeta = IndexWriter(CatalogDatabase.getInstance(reactApplicationContext)).readCatalogMeta()
    val meta =
        Arguments.createMap().apply {
          putDouble("schemaVersion", catalogMeta.schemaVersion.toDouble())
          putBoolean("fullRescanRequired", catalogMeta.fullRescanRequired)
        }
    promise.resolve(meta)
  }

  @DoNotStrip
  override fun getCatalogSnapshot(promise: Promise) {
    try {
      val catalogSnapshot =
          CatalogReader(CatalogDatabase.getInstance(reactApplicationContext)).readSnapshot()
      val bridgeMap = CatalogSnapshotBridgeMapper.toReadableMap(catalogSnapshot)
      promise.resolve(toWritableMap(bridgeMap))
    } catch (error: Exception) {
      promise.reject(CODE_CATALOG_READ_FAILED, error.message, error)
    }
  }

  private fun rejectNotImplemented(method: String, promise: Promise) {
    promise.reject(
        CODE_NOT_IMPLEMENTED,
        "$method is not implemented until ScanEngine pipeline tasks (M1-03+)",
    )
  }

  companion object {
    const val NAME: String = NativeScanEngineSpec.NAME
    private const val CODE_NOT_IMPLEMENTED = "SCANENGINE_NOT_IMPLEMENTED"
    private const val CODE_SCAN_START_FAILED = "SCAN_START_FAILED"
    private const val CODE_SCAN_CONTROL_FAILED = "SCAN_CONTROL_FAILED"
    private const val CODE_CATALOG_READ_FAILED = "CATALOG_READ_FAILED"
    private const val CODE_RESUMABLE_SCAN_FAILED = "RESUMABLE_SCAN_FAILED"
    private const val CODE_DELETE_INVALID = "DELETE_INVALID_COMMAND"
  }
}
