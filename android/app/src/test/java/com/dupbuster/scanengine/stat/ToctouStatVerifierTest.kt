package com.dupbuster.scanengine.stat

import android.net.Uri
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ToctouStatVerifierTest {

  private val contentUri =
      Uri.parse("content://test/documents/document/primary%3Afile.bin")

  @Test
  fun verifyBaseline_matchingSizeAndMtime_returnsConsistent() {
    val staged = staged(sizeBytes = 100, mtimeNs = 2_000)
    val verifier =
        ToctouStatVerifier(
            object : FileStatReader {
              override fun readStat(uri: Uri): FileStatReadOutcome =
                  FileStatReadOutcome.Ok(
                      FileStat(
                          sizeBytes = 100,
                          mtimeNs = 2_000,
                          inode = null,
                          deviceId = null,
                          isSymlink = false,
                      ),
                  )
            },
        )

    assertTrue(verifier.verifyBaseline(staged) is ToctouVerifyOutcome.Consistent)
  }

  @Test
  fun verifyBaseline_sizeChanged_returnsChangedWithFreshStat() {
    val staged = staged(sizeBytes = 100, mtimeNs = 2_000)
    val fresh =
        FileStat(
            sizeBytes = 200,
            mtimeNs = 2_000,
            inode = null,
            deviceId = null,
            isSymlink = false,
        )
    val verifier =
        ToctouStatVerifier(
            object : FileStatReader {
              override fun readStat(uri: Uri): FileStatReadOutcome = FileStatReadOutcome.Ok(fresh)
            },
        )

    val outcome = verifier.verifyBaseline(staged)
    assertTrue(outcome is ToctouVerifyOutcome.Changed)
    assertEquals(200L, (outcome as ToctouVerifyOutcome.Changed).freshStat.sizeBytes)
  }

  @Test
  fun applyFreshStat_updatesBaselineFields() {
    val staged = staged(sizeBytes = 10, mtimeNs = 1)
    val updated =
        ToctouStatVerifier(
                object : FileStatReader {
                  override fun readStat(uri: Uri): FileStatReadOutcome =
                      FileStatReadOutcome.IoFailure
                },
            )
            .applyFreshStat(
                staged,
                FileStat(
                    sizeBytes = 99,
                    mtimeNs = 88,
                    inode = 7L,
                    deviceId = 3L,
                    isSymlink = false,
                ),
            )

    assertEquals(99L, updated.sizeBytes)
    assertEquals(88L, updated.mtimeNs)
    assertEquals(7L, updated.inode)
    assertEquals(3L, updated.deviceId)
  }

  private fun staged(sizeBytes: Long, mtimeNs: Long): StagedFile =
      StagedFile(
          discovered =
              DiscoveredEntry(
                  contentUri = contentUri,
                  scanRootId = 1L,
                  generation = 1,
                  displayName = "file.bin",
                  mediaTypeHint = MediaTypeHint.OTHER,
                  sizeBytes = sizeBytes,
                  mtimeNs = mtimeNs,
              ),
          sizeBytes = sizeBytes,
          mtimeNs = mtimeNs,
          inode = null,
          deviceId = null,
          isSymlink = false,
          mediaTypeHint = MediaTypeHint.OTHER,
      )
}
