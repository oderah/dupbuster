package com.dupbuster.scanengine.scan

import android.content.Context
import com.dupbuster.scanengine.bridge.ScanProgressBridge
import com.dupbuster.scanengine.hash.ContentResolverImageBitmapExtractor
import com.dupbuster.scanengine.hash.ContentResolverVideoFrameExtractor
import com.dupbuster.scanengine.hash.ContentResolverFileContentReader
import com.dupbuster.scanengine.hash.HashPipeline
import com.dupbuster.scanengine.hash.ImageFingerprinter
import com.dupbuster.scanengine.hash.VideoFingerprinter
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.CheckpointStore
import com.dupbuster.scanengine.index.Grouper
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.index.SqliteDurationBucketIndex
import com.dupbuster.scanengine.index.SqliteSizeBucketIndex
import com.dupbuster.scanengine.foreground.AndroidScanForegroundController
import com.dupbuster.scanengine.foreground.AndroidScanForegroundServiceClient
import com.dupbuster.scanengine.stat.ContentResolverFileStatReader
import com.dupbuster.scanengine.stat.StatStage
import com.dupbuster.scanengine.stat.ToctouStatVerifier
import com.facebook.react.bridge.ReadableMap

object ScanOrchestratorFactory {

  fun create(
      context: Context,
      emitProgress: (ReadableMap) -> Unit,
      emitError: (ReadableMap) -> Unit,
  ): ScanOrchestrator {
    val appContext = context.applicationContext
    val database = CatalogDatabase.getInstance(appContext)
    val indexWriter = IndexWriter(database)
    val checkpointStore = CheckpointStore(database)
    val progressBridge = ScanProgressBridge(emitProgress = emitProgress)
    val openFileRegistry = ScanOpenFileRegistry()
    val foregroundController =
        AndroidScanForegroundController(AndroidScanForegroundServiceClient(appContext))
    val fileStatReader = ContentResolverFileStatReader(appContext, openFileRegistry)
    val contentReader = ContentResolverFileContentReader(appContext, openFileRegistry)
    return ScanOrchestrator(
        indexWriter = indexWriter,
        checkpointStore = checkpointStore,
        grouper = Grouper(database),
        discoveryRunner = ProductionScanDiscoveryRunner(appContext),
        statFile = { entry, grant ->
          StatStage(appContext, fileStatReader = fileStatReader).stat(entry, grant)
        },
        hashPipelineFactory = { writer ->
          HashPipeline(
              appContext,
              contentReader = contentReader,
              sizeBucketIndex = SqliteSizeBucketIndex(writer),
              durationBucketIndex = SqliteDurationBucketIndex(writer),
              imageFingerprinter =
                  ImageFingerprinter(ContentResolverImageBitmapExtractor(appContext)),
              videoFingerprinter =
                  VideoFingerprinter(ContentResolverVideoFrameExtractor(appContext)),
          )
        },
        progressBridge = progressBridge,
        emitError = emitError,
        toctouVerifier = ToctouStatVerifier(fileStatReader),
        openFileRegistry = openFileRegistry,
        foregroundController = foregroundController,
    )
  }
}
