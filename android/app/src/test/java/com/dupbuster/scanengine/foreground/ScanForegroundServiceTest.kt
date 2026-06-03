package com.dupbuster.scanengine.foreground

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ScanForegroundServiceTest {

  @Test
  fun onCreate_registersStableNotificationChannel() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    Robolectric.setupService(ScanForegroundService::class.java)

    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = manager.getNotificationChannel(ScanForegroundConstants.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(ScanForegroundNotificationCopy.CHANNEL_NAME, channel!!.name)
  }

  @Test
  fun startAction_postsForegroundNotificationWithFrozenCopy() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = Robolectric.setupService(ScanForegroundService::class.java)
    service.onStartCommand(
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_START
        },
        0,
        1,
    )

    val manager = context.getSystemService(NotificationManager::class.java)
    val active =
        manager.activeNotifications.firstOrNull { it.id == ScanForegroundConstants.NOTIFICATION_ID }
    assertNotNull(active)
    assertEquals(
        ScanForegroundNotificationCopy.TITLE,
        active!!.notification.extras.getCharSequence("android.title").toString(),
    )
  }

  @Test
  fun updateAction_refreshesNotificationBody() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = Robolectric.setupService(ScanForegroundService::class.java)
    service.onStartCommand(
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_START
        },
        0,
        1,
    )
    service.onStartCommand(
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_UPDATE
          putExtra(ScanForegroundService.EXTRA_FILES_PROCESSED, 40)
          putExtra(ScanForegroundService.EXTRA_FILES_TOTAL_KNOWN, 80)
        },
        0,
        2,
    )

    val manager = context.getSystemService(NotificationManager::class.java)
    val active =
        manager.activeNotifications.firstOrNull { it.id == ScanForegroundConstants.NOTIFICATION_ID }
    assertNotNull(active)
    assertEquals(
        ScanForegroundNotificationCopy.formatBody(40, 80),
        active!!.notification.extras.getCharSequence("android.text").toString(),
    )
  }

  @Test
  fun stopAction_clearsNotification() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = Robolectric.setupService(ScanForegroundService::class.java)
    service.onStartCommand(
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_START
        },
        0,
        1,
    )
    service.onStartCommand(
        Intent(context, ScanForegroundService::class.java).apply {
          action = ScanForegroundService.ACTION_STOP
        },
        0,
        2,
    )

    val manager = context.getSystemService(NotificationManager::class.java)
    val active =
        manager.activeNotifications.firstOrNull { it.id == ScanForegroundConstants.NOTIFICATION_ID }
    assertEquals(null, active)
  }
}
