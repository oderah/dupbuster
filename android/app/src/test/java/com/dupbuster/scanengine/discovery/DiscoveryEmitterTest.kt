package com.dupbuster.scanengine.discovery

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.security.UriValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DiscoveryEmitterTest {

  private lateinit var context: Context
  private val treeUri =
      Uri.parse(
          "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
      )
  private val grant = ScanRootGrant(uriGrant = treeUri, mode = ScanRootMode.USER_SELECTED)

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun emitModeA_walksNestedSafTree_andSkipsDirectories() {
    val fakeQuery =
        object : SafChildDocumentsQuery {
          override fun queryChildren(treeUri: Uri, parentDocumentId: String): List<SafChildRow> {
            return when (parentDocumentId) {
              "primary:Documents" ->
                  listOf(
                      SafChildRow(
                          documentId = "primary:Documents/photos",
                          displayName = "photos",
                          mimeType = "vnd.android.document/directory",
                          sizeBytes = 0,
                          lastModifiedMs = 0,
                          isDirectory = true,
                      ),
                      SafChildRow(
                          documentId = "primary:Documents/readme.txt",
                          displayName = "readme.txt",
                          mimeType = "text/plain",
                          sizeBytes = 12,
                          lastModifiedMs = 1_700_000_000_000L,
                          isDirectory = false,
                      ),
                  )
              "primary:Documents/photos" ->
                  listOf(
                      SafChildRow(
                          documentId = "primary:Documents/photos/a.jpg",
                          displayName = "a.jpg",
                          mimeType = "image/jpeg",
                          sizeBytes = 1024,
                          lastModifiedMs = 1_700_000_000_001L,
                          isDirectory = false,
                      ),
                  )
              else -> emptyList()
            }
          }
        }

    val emitter = DiscoveryEmitter(context, childQuery = fakeQuery, uriValidator = UriValidator(context))
    val entries = mutableListOf<DiscoveredEntry>()
    val result =
        emitter.emitModeA(
            DiscoveryRequest(scanRootId = 1L, generation = 7, grant = grant),
            DiscoveryEntryConsumer { entries.add(it) },
        )

    assertEquals(2, result.entriesEmitted)
    assertEquals(0, result.entriesDenied)
    assertTrue(result.directoriesVisited >= 2)
    assertEquals(setOf("readme.txt", "a.jpg"), entries.map { it.displayName }.toSet())
    assertEquals(MediaTypeHint.TEXT, entries.first { it.displayName == "readme.txt" }.mediaTypeHint)
    assertEquals(MediaTypeHint.IMAGE, entries.first { it.displayName == "a.jpg" }.mediaTypeHint)
    entries.forEach {
      assertEquals(1L, it.scanRootId)
      assertEquals(7, it.generation)
      assertTrue(it.contentUri.toString().startsWith("content://"))
    }
  }

  @Test
  fun emitModeA_deniesInvalidSafDocuments() {
    val fakeQuery =
        object : SafChildDocumentsQuery {
          override fun queryChildren(treeUri: Uri, parentDocumentId: String): List<SafChildRow> {
            return listOf(
                SafChildRow(
                    documentId = "primary:Documents/../secret",
                    displayName = "secret.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 100,
                    lastModifiedMs = 0,
                    isDirectory = false,
                ),
            )
          }
        }

    val emitter = DiscoveryEmitter(context, childQuery = fakeQuery, uriValidator = UriValidator(context))
    val result =
        emitter.emitModeA(
            DiscoveryRequest(scanRootId = 2L, generation = 1, grant = grant),
            DiscoveryEntryConsumer {},
        )

    assertEquals(0, result.entriesEmitted)
    assertEquals(1, result.entriesDenied)
  }

  @Test
  fun emitModeA_honoursCancel() {
    val fakeQuery =
        object : SafChildDocumentsQuery {
          override fun queryChildren(treeUri: Uri, parentDocumentId: String): List<SafChildRow> {
            return listOf(
                SafChildRow(
                    documentId = "primary:Documents/a.txt",
                    displayName = "a.txt",
                    mimeType = "text/plain",
                    sizeBytes = 1,
                    lastModifiedMs = 0,
                    isDirectory = false,
                ),
            )
          }
        }

    val emitter = DiscoveryEmitter(context, childQuery = fakeQuery, uriValidator = UriValidator(context))
    val result =
        emitter.emitModeA(
            DiscoveryRequest(scanRootId = 1L, generation = 1, grant = grant),
            DiscoveryEntryConsumer {},
            isCancelled = { true },
        )

    assertTrue(result.cancelled)
    assertEquals(0, result.entriesEmitted)
  }
}
