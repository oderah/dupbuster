package com.dupbuster.scanengine.foreground

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Production [ScanForegroundServiceClient] using application context. */
class AndroidScanForegroundServiceClient(private val context: Context) : ScanForegroundServiceClient {
  override fun start() {
    val intent =
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_START
        }
    ContextCompat.startForegroundService(context, intent)
  }

  override fun update(filesProcessed: Int, filesTotalKnown: Int?) {
    val intent =
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_UPDATE
          putExtra(ScanForegroundService.EXTRA_FILES_PROCESSED, filesProcessed)
          if (filesTotalKnown != null) {
            putExtra(ScanForegroundService.EXTRA_FILES_TOTAL_KNOWN, filesTotalKnown)
          }
        }
    context.startService(intent)
  }

  override fun stop() {
    val intent =
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_STOP
        }
    context.startService(intent)
  }
}
