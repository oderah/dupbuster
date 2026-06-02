package com.dupbuster.scanengine.delete

import android.content.IntentSender

/** Launches platform delete confirmation UI (MediaStore / RecoverableSecurity). */
fun interface DeleteConfirmationLauncher {
  fun launchForResult(intentSender: IntentSender): Boolean
}
