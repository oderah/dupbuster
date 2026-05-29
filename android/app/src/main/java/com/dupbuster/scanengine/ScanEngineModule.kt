package com.dupbuster.scanengine

import com.facebook.proguard.annotations.DoNotStrip
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.IndexWriter
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule

/**
 * ScanEngine Turbo Module stub (M1-02). UriValidator M1-03; DiscoveryEmitter mode A M1-04.
 * Bridge contract: [src/native/NativeScanEngine.ts]
 */
@DoNotStrip
@ReactModule(name = ScanEngineModule.NAME)
class ScanEngineModule(reactContext: ReactApplicationContext) :
    NativeScanEngineSpec(reactContext) {

  override fun getName(): String = NAME

  @DoNotStrip
  override fun startScan(options: ReadableMap, promise: Promise) {
    rejectNotImplemented("startScan", promise)
  }

  @DoNotStrip
  override fun pauseScan(scanRunId: Double, promise: Promise) {
    rejectNotImplemented("pauseScan", promise)
  }

  @DoNotStrip
  override fun resumeScan(scanRunId: Double, promise: Promise) {
    rejectNotImplemented("resumeScan", promise)
  }

  @DoNotStrip
  override fun cancelScan(scanRunId: Double, promise: Promise) {
    rejectNotImplemented("cancelScan", promise)
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

  private fun rejectNotImplemented(method: String, promise: Promise) {
    promise.reject(
        CODE_NOT_IMPLEMENTED,
        "$method is not implemented until ScanEngine pipeline tasks (M1-03+)",
    )
  }

  companion object {
    const val NAME: String = NativeScanEngineSpec.NAME
    private const val CODE_NOT_IMPLEMENTED = "SCANENGINE_NOT_IMPLEMENTED"
  }
}
