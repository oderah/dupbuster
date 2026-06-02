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
class DiscoveryEmitterModeBTest {

  private lateinit var context: Context
  private val platformGrant = PlatformDiscoveryGrant.grant()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun emitModeB_queriesMediaStoreUnion_andDedupesByUri() {
    val imageUri =
        Uri.parse("content://com.android.providers.media.documents/document/image%3A100")
    val videoUri =
        Uri.parse("content://com.android.providers.media.documents/document/video%3A200")
    val fakeMediaStore =
        object : MediaStoreDiscoveryQuery {
          override fun queryCollection(kind: MediaStoreCollectionKind): List<MediaStoreRow> {
            return when (kind) {
              MediaStoreCollectionKind.IMAGE ->
                  listOf(
                      MediaStoreRow(
                          contentUri = imageUri,
                          displayName = "photo.jpg",
                          mimeType = "image/jpeg",
                          sizeBytes = 2048,
                          lastModifiedMs = 1_700_000_000_000L,
                          collectionKind = MediaStoreCollectionKind.IMAGE,
                      ),
                      MediaStoreRow(
                          contentUri = imageUri,
                          displayName = "photo-dup.jpg",
                          mimeType = "image/jpeg",
                          sizeBytes = 2048,
                          lastModifiedMs = 1_700_000_000_000L,
                          collectionKind = MediaStoreCollectionKind.IMAGE,
                      ),
                  )
              MediaStoreCollectionKind.VIDEO ->
                  listOf(
                      MediaStoreRow(
                          contentUri = videoUri,
                          displayName = "clip.mp4",
                          mimeType = "video/mp4",
                          sizeBytes = 4096,
                          lastModifiedMs = 1_700_000_000_001L,
                          collectionKind = MediaStoreCollectionKind.VIDEO,
                      ),
                  )
              else -> emptyList()
            }
          }
        }

    val emitter =
        DiscoveryEmitter(
            context,
            mediaStoreQuery = fakeMediaStore,
            uriValidator = UriValidator(context),
        )
    val entries = mutableListOf<DiscoveredEntry>()
    val result =
        emitter.emitModeB(
            PlatformDiscoveryRequest(
                scanRootId = 10L,
                generation = 2,
                grant = platformGrant,
            ),
            DiscoveryEntryConsumer { entries.add(it) },
        )

    assertEquals(2, result.entriesEmitted)
    assertEquals(0, result.entriesDenied)
    assertEquals(setOf("photo.jpg", "clip.mp4"), entries.map { it.displayName }.toSet())
    assertEquals(MediaTypeHint.IMAGE, entries.first { it.displayName == "photo.jpg" }.mediaTypeHint)
    assertEquals(MediaTypeHint.VIDEO, entries.first { it.displayName == "clip.mp4" }.mediaTypeHint)
    entries.forEach {
      assertEquals(10L, it.scanRootId)
      assertEquals(2, it.generation)
    }
  }

  @Test
  fun emitModeB_downloadJpegInDownloadsCollection_usesImageMediaHint() {
    val downloadUri =
        Uri.parse("content://com.android.providers.media.documents/document/downloads%3A42")
    val fakeMediaStore =
        object : MediaStoreDiscoveryQuery {
          override fun queryCollection(kind: MediaStoreCollectionKind): List<MediaStoreRow> {
            if (kind != MediaStoreCollectionKind.DOWNLOAD) {
              return emptyList()
            }
            return listOf(
                MediaStoreRow(
                    contentUri = downloadUri,
                    displayName = "download.jpeg",
                    mimeType = "image/jpeg",
                    sizeBytes = 4_970,
                    lastModifiedMs = 1_700_000_000_000L,
                    collectionKind = MediaStoreCollectionKind.DOWNLOAD,
                ),
            )
          }
        }

    val emitter =
        DiscoveryEmitter(context, mediaStoreQuery = fakeMediaStore, uriValidator = UriValidator(context))
    val entries = mutableListOf<DiscoveredEntry>()
    emitter.emitModeB(
        PlatformDiscoveryRequest(scanRootId = 1L, generation = 1, grant = platformGrant),
        DiscoveryEntryConsumer { entries.add(it) },
    )

    assertEquals(1, entries.size)
    assertEquals(MediaTypeHint.IMAGE, entries[0].mediaTypeHint)
  }

  @Test
  fun emitModeB_deniesNonMediaStoreUri() {
    val fakeMediaStore =
        object : MediaStoreDiscoveryQuery {
          override fun queryCollection(kind: MediaStoreCollectionKind): List<MediaStoreRow> {
            if (kind != MediaStoreCollectionKind.AUDIO) {
              return emptyList()
            }
            return listOf(
                MediaStoreRow(
                    contentUri = Uri.parse("content://evil.provider/document/track.mp3"),
                    displayName = "track.mp3",
                    mimeType = "audio/mpeg",
                    sizeBytes = 100,
                    lastModifiedMs = 0,
                    collectionKind = MediaStoreCollectionKind.AUDIO,
                ),
            )
          }
        }

    val emitter =
        DiscoveryEmitter(context, mediaStoreQuery = fakeMediaStore, uriValidator = UriValidator(context))
    val result =
        emitter.emitModeB(
            PlatformDiscoveryRequest(scanRootId = 1L, generation = 1, grant = platformGrant),
            DiscoveryEntryConsumer {},
        )

    assertEquals(0, result.entriesEmitted)
    assertEquals(1, result.entriesDenied)
  }

  @Test
  fun emitModeB_honoursCancel() {
    val fakeMediaStore =
        object : MediaStoreDiscoveryQuery {
          override fun queryCollection(kind: MediaStoreCollectionKind): List<MediaStoreRow> {
            return listOf(
                MediaStoreRow(
                    contentUri =
                        Uri.parse(
                            "content://com.android.providers.media.documents/document/image%3A1",
                        ),
                    displayName = "a.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 1,
                    lastModifiedMs = 0,
                    collectionKind = MediaStoreCollectionKind.IMAGE,
                ),
            )
          }
        }

    val emitter =
        DiscoveryEmitter(context, mediaStoreQuery = fakeMediaStore, uriValidator = UriValidator(context))
    val result =
        emitter.emitModeB(
            PlatformDiscoveryRequest(scanRootId = 1L, generation = 1, grant = platformGrant),
            DiscoveryEntryConsumer {},
            isCancelled = { true },
        )

    assertTrue(result.cancelled)
    assertEquals(0, result.entriesEmitted)
  }
}
