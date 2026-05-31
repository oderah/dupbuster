package com.dupbuster.scanengine

import com.facebook.proguard.annotations.DoNotStrip
import com.dupbuster.scanengine.bridge.CatalogSnapshotBridgeMapper
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.CatalogReader
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.scan.ScanOrchestrator
import com.dupbuster.scanengine.scan.ScanOrchestratorFactory
import com.dupbuster.scanengine.scan.ScanStartRequestParser
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
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
        ReadableType.String -> nativeMap.putString(key, payload.getString(key))
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
    rejectNotImplemented("deleteDuplicates", promise)
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
      val snapshot =
          CatalogSnapshotBridgeMapper.toReadableMap(
              CatalogReader(CatalogDatabase.getInstance(reactApplicationContext)).readSnapshot(),
          )
      promise.resolve(snapshot)
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
  }
}
