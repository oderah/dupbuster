package com.dupbuster.scanengine.delete

import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.index.FileEntryDeleteTarget
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AndroidPlatformFileDeleterTest {

  private lateinit var deleter: AndroidPlatformFileDeleter
  private var mediaStoreBatchInvoked = false
  private var safDeleteInvoked = false

  @Before
  fun setUp() {
    mediaStoreBatchInvoked = false
    safDeleteInvoked = false
    deleter =
        AndroidPlatformFileDeleter(
            context = ApplicationProvider.getApplicationContext(),
            deleteConfirmationLauncher = null,
            contentDeleteGateway =
                object : ContentDeleteGateway {
                  override fun deleteSafDocument(uri: Uri): Boolean {
                    safDeleteInvoked = true
                    return true
                  }

                  override fun deleteMediaStoreUris(uris: List<Uri>): Boolean {
                    mediaStoreBatchInvoked = true
                    return uris.isNotEmpty()
                  }
                },
        )
  }

  @Test
  fun deleteTargets_safDocument_usesSafGateway() {
    val uri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fa.jpg",
        )
    val target = target(42L, uri, ScanRootMode.USER_SELECTED)

    val deleted = deleter.deleteTargets(listOf(target))

    assertTrue(safDeleteInvoked)
    assertFalse(mediaStoreBatchInvoked)
    assertEquals(setOf(42L), deleted)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun deleteTargets_mediaStore_batchesThroughGateway() {
    val uri = Uri.parse("content://media/external/images/media/99")
    val target =
        FileEntryDeleteTarget(
            fileEntryId = 7L,
            uriOrPath = uri.toString(),
            grant =
                ScanRootGrant(
                    uriGrant = Uri.parse("content://dupbuster/platform-discovery"),
                    mode = ScanRootMode.PLATFORM_DISCOVERY,
                ),
        )

    val deleted = deleter.deleteTargets(listOf(target))

    assertFalse(safDeleteInvoked)
    assertTrue(mediaStoreBatchInvoked)
    assertEquals(setOf(7L), deleted)
  }

  @Test
  fun shouldUseMediaStoreDelete_onlyForPlatformDiscoveryMediaUris() {
    val mediaUri = Uri.parse("content://media/external/images/media/1")
    val safUri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fa.jpg",
        )
    val platformGrant =
        ScanRootGrant(
            uriGrant = Uri.parse("content://dupbuster/platform-discovery"),
            mode = ScanRootMode.PLATFORM_DISCOVERY,
        )
    val userGrant =
        ScanRootGrant(
            uriGrant =
                Uri.parse(
                    "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
                ),
            mode = ScanRootMode.USER_SELECTED,
        )

    assertTrue(
        deleter.shouldUseMediaStoreDelete(
            mediaUri,
            FileEntryDeleteTarget(1L, mediaUri.toString(), platformGrant),
        ),
    )
    assertFalse(
        deleter.shouldUseMediaStoreDelete(
            safUri,
            FileEntryDeleteTarget(2L, safUri.toString(), platformGrant),
        ),
    )
    assertFalse(
        deleter.shouldUseMediaStoreDelete(
            mediaUri,
            FileEntryDeleteTarget(3L, mediaUri.toString(), userGrant),
        ),
    )
  }

  @Test
  fun deleteTargets_gatewayMediaStoreFailure_doesNotReportDeleted() {
    val failingDeleter =
        AndroidPlatformFileDeleter(
            context = ApplicationProvider.getApplicationContext(),
            deleteConfirmationLauncher = null,
            contentDeleteGateway =
                object : ContentDeleteGateway {
                  override fun deleteSafDocument(uri: Uri): Boolean = true

                  override fun deleteMediaStoreUris(uris: List<Uri>): Boolean = false
                },
        )
    val uri = Uri.parse("content://media/external/images/media/99")
    val target =
        FileEntryDeleteTarget(
            fileEntryId = 3L,
            uriOrPath = uri.toString(),
            grant =
                ScanRootGrant(
                    uriGrant = Uri.parse("content://dupbuster/platform-discovery"),
                    mode = ScanRootMode.PLATFORM_DISCOVERY,
                ),
        )

    val deleted = failingDeleter.deleteTargets(listOf(target))

    assertTrue(deleted.isEmpty())
  }

  private fun target(fileEntryId: Long, uri: Uri, mode: ScanRootMode): FileEntryDeleteTarget {
    val grantUri =
        when (mode) {
          ScanRootMode.USER_SELECTED -> uri
          ScanRootMode.PLATFORM_DISCOVERY ->
              Uri.parse("content://dupbuster/platform-discovery")
        }
    return FileEntryDeleteTarget(
        fileEntryId = fileEntryId,
        uriOrPath = uri.toString(),
        grant = ScanRootGrant(uriGrant = grantUri, mode = mode),
    )
  }
}
