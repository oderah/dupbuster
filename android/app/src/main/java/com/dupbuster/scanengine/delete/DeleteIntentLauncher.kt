package com.dupbuster.scanengine.delete

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Launches MediaStore / RecoverableSecurity delete confirmation on the foreground Activity
 * (FR-AC-07). Blocks the delete executor until the user completes the system sheet.
 */
class DeleteIntentLauncher(
    private val reactContext: ReactApplicationContext,
    private val timeoutSeconds: Long = 120L,
) : DeleteConfirmationLauncher {

  private val completionLock = Any()
  private var pendingLatch: CountDownLatch? = null
  private var pendingSuccess = AtomicBoolean(false)

  init {
    reactContext.addActivityEventListener(
        object : BaseActivityEventListener() {
          override fun onActivityResult(
              activity: Activity,
              requestCode: Int,
              resultCode: Int,
              data: Intent?,
          ) {
            if (requestCode != REQUEST_CODE_DELETE) {
              return
            }
            synchronized(completionLock) {
              pendingSuccess.set(resultCode == Activity.RESULT_OK)
              pendingLatch?.countDown()
            }
          }
        },
    )
  }

  /** @return true when the user confirmed and the platform reported success. */
  override fun launchForResult(intentSender: IntentSender): Boolean {
    val activity =
        reactContext.currentActivity
            ?: return false

    val latch = CountDownLatch(1)
    synchronized(completionLock) {
      pendingSuccess.set(false)
      pendingLatch = latch
    }

    UiThreadUtil.runOnUiThread {
      try {
        @Suppress("DEPRECATION")
        activity.startIntentSenderForResult(
            intentSender,
            REQUEST_CODE_DELETE,
            null,
            0,
            0,
            0,
        )
      } catch (_: Exception) {
        synchronized(completionLock) {
          pendingLatch?.countDown()
        }
      }
    }

    val completed = latch.await(timeoutSeconds, TimeUnit.SECONDS)
  synchronized(completionLock) {
      pendingLatch = null
    }
    return completed && pendingSuccess.get()
  }

  companion object {
    const val REQUEST_CODE_DELETE = 0xD4B5
  }
}
