package com.dupbuster.scanengine.delete

import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DefaultContentDeleteGatewayTest {

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun deleteMediaStoreUris_withoutLauncher_failsClosed() {
    val gateway =
        DefaultContentDeleteGateway(
            context = ApplicationProvider.getApplicationContext(),
            deleteConfirmationLauncher = null,
        )
    val uri = Uri.parse("content://media/external/images/media/42")

    assertFalse(gateway.deleteMediaStoreUris(listOf(uri)))
  }
}
