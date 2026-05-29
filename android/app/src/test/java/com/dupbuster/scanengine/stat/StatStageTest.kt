package com.dupbuster.scanengine.stat

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.security.UriValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StatStageTest {

  private lateinit var context: Context
  private val grant =
      ScanRootGrant(
          uriGrant =
              Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments"),
          mode = ScanRootMode.USER_SELECTED,
      )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun stat_returnsFreshMetadata_whenReaderSucceeds() {
    val contentUri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Freadme.txt",
        )
    val entry = discoveredEntry(contentUri, sizeBytes = 12, mtimeNs = 1000)

    val fakeReader =
        object : FileStatReader {
          override fun readStat(uri: Uri): FileStatReadOutcome {
            assertEquals(contentUri, uri)
            return FileStatReadOutcome.Ok(
                FileStat(
                    sizeBytes = 4096,
                    mtimeNs = 2_000_000_000L,
                    inode = 42L,
                    deviceId = 7L,
                    isSymlink = false,
                ),
            )
          }
        }

    val stage = StatStage(context, fileStatReader = fakeReader, uriValidator = UriValidator(context))
    val result = stage.stat(entry, grant)

    val success = result as StatResult.Success
    assertEquals(4096, success.staged.sizeBytes)
    assertEquals(2_000_000_000L, success.staged.mtimeNs)
    assertEquals(42L, success.staged.inode)
    assertEquals(7L, success.staged.deviceId)
    assertFalse(success.staged.isSymlink)
    assertEquals(MediaTypeHint.TEXT, success.staged.mediaTypeHint)
    assertEquals(entry, success.staged.discovered)
  }

  @Test
  fun stat_marksSymlink_whenReaderReportsSymlink() {
    val contentUri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Flink.txt",
        )
    val fakeReader =
        object : FileStatReader {
          override fun readStat(uri: Uri): FileStatReadOutcome =
              FileStatReadOutcome.Ok(
                  FileStat(
                      sizeBytes = 1,
                      mtimeNs = 1,
                      inode = 1,
                      deviceId = 1,
                      isSymlink = true,
                  ),
              )
        }

    val stage = StatStage(context, fileStatReader = fakeReader, uriValidator = UriValidator(context))
    val result = stage.stat(discoveredEntry(contentUri), grant)

    val success = result as StatResult.Success
    assertTrue(success.staged.isSymlink)
  }

  @Test
  fun stat_deniesOutOfGrantUri() {
    val contentUri = Uri.parse("content://evil.provider/document/out.txt")
    val fakeReader =
        object : FileStatReader {
          override fun readStat(uri: Uri): FileStatReadOutcome {
            throw AssertionError("reader must not run when UriValidator denies")
          }
        }

    val stage = StatStage(context, fileStatReader = fakeReader, uriValidator = UriValidator(context))
    val result = stage.stat(discoveredEntry(contentUri), grant)

    val unscannable = result as StatResult.Unscannable
    assertEquals(UnscannableReason.PERMISSION_DENIED, unscannable.reason)
  }

  @Test
  fun stat_returnsUnscannable_whenReaderFails() {
    val contentUri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fmissing.txt",
        )
    val fakeReader =
        object : FileStatReader {
          override fun readStat(uri: Uri): FileStatReadOutcome = FileStatReadOutcome.IoFailure
        }

    val stage = StatStage(context, fileStatReader = fakeReader, uriValidator = UriValidator(context))
    val result = stage.stat(discoveredEntry(contentUri), grant)

    assertTrue(result is StatResult.Unscannable)
  }

  private fun discoveredEntry(
      uri: Uri,
      sizeBytes: Long = 0,
      mtimeNs: Long = 0,
  ): DiscoveredEntry =
      DiscoveredEntry(
          contentUri = uri,
          scanRootId = 1L,
          generation = 1,
          displayName = "file.txt",
          mediaTypeHint = MediaTypeHint.TEXT,
          sizeBytes = sizeBytes,
          mtimeNs = mtimeNs,
      )
}
