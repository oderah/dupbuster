package com.dupbuster.scanengine.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.dupbuster.MainActivity
import com.dupbuster.R

/**
 * Android FGS for active duplicate scans (M4-01).
 * Type `dataSync`; channel [ScanForegroundConstants.CHANNEL_ID].
 */
class ScanForegroundService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> promoteForeground(buildNotification(filesProcessed = 0, filesTotalKnown = null))
      ACTION_UPDATE -> {
        val filesProcessed = intent.getIntExtra(EXTRA_FILES_PROCESSED, 0)
        val filesTotalKnown =
            if (intent.hasExtra(EXTRA_FILES_TOTAL_KNOWN)) {
              intent.getIntExtra(EXTRA_FILES_TOTAL_KNOWN, 0)
            } else {
              null
            }
        val notification = buildNotification(filesProcessed, filesTotalKnown)
        promoteForeground(notification)
        getSystemService(NotificationManager::class.java).notify(
            ScanForegroundConstants.NOTIFICATION_ID,
            notification,
        )
      }
      ACTION_STOP -> {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
      else -> Unit
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    getSystemService(NotificationManager::class.java)
        .cancel(ScanForegroundConstants.NOTIFICATION_ID)
    super.onDestroy()
  }

  private fun promoteForeground(notification: Notification) {
    ServiceCompat.startForeground(
        this,
        ScanForegroundConstants.NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }
    val manager = getSystemService(NotificationManager::class.java)
    val existing = manager.getNotificationChannel(ScanForegroundConstants.CHANNEL_ID)
    if (existing != null) {
      return
    }
    val channel =
        NotificationChannel(
            ScanForegroundConstants.CHANNEL_ID,
            ScanForegroundNotificationCopy.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = ScanForegroundNotificationCopy.CHANNEL_NAME
          setShowBadge(false)
        }
    manager.createNotificationChannel(channel)
  }

  private fun buildNotification(filesProcessed: Int, filesTotalKnown: Int?): Notification {
    val launchIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    return NotificationCompat.Builder(this, ScanForegroundConstants.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(ScanForegroundNotificationCopy.TITLE)
        .setContentText(
            ScanForegroundNotificationCopy.formatBody(filesProcessed, filesTotalKnown),
        )
        .setContentIntent(launchIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
  }

  companion object {
    const val ACTION_START: String = "com.dupbuster.scanengine.foreground.START"
    const val ACTION_UPDATE: String = "com.dupbuster.scanengine.foreground.UPDATE"
    const val ACTION_STOP: String = "com.dupbuster.scanengine.foreground.STOP"
    const val EXTRA_FILES_PROCESSED: String = "filesProcessed"
    const val EXTRA_FILES_TOTAL_KNOWN: String = "filesTotalKnown"
  }
}
