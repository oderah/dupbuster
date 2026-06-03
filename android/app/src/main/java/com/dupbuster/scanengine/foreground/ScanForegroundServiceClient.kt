package com.dupbuster.scanengine.foreground

/** Starts, updates, and stops [ScanForegroundService]. */
interface ScanForegroundServiceClient {
  fun start()

  fun update(filesProcessed: Int, filesTotalKnown: Int?)

  fun stop()
}
