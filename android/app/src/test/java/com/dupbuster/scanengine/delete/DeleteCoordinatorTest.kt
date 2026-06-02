package com.dupbuster.scanengine.delete

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.Grouper
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.security.UriValidator
import com.dupbuster.scanengine.stat.StagedFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeleteCoordinatorTest {

  private lateinit var context: Context
  private lateinit var database: CatalogDatabase
  private lateinit var writer: IndexWriter
  private lateinit var grouper: Grouper
  private var rootId: Long = 0

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = CatalogDatabase.inMemory(context)
    database.writable()
    writer = IndexWriter(database)
    grouper = Grouper(database)
    rootId =
        writer.insertScanRoot(
            uriOrGrant =
                "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
            mode = ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun runDelete_withFakeDeleter_updatesCatalogAndReturnsCounts() {
    val uriA =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fphotos%2Fa.jpg",
        )
    val uriB =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fphotos%2Fb.jpg",
        )
    val idA = upsertHashedEntry(staged(uriA), "hash-dup", generation = 1)
    val idB = upsertHashedEntry(staged(uriB), "hash-dup", generation = 1)
    grouper.rebuildDuplicateGroups()
    val groupId = grouper.firstGroupId()!!

    val coordinator =
        DeleteCoordinator(
            indexWriter = writer,
            uriValidator = UriValidator(context),
            platformFileDeleter = FakePlatformFileDeleter(),
        )

    val result =
        coordinator.runDelete(
            DeleteDuplicatesCommand(
                groupId = groupId,
                keeperFileEntryId = idA,
                deleteFileEntryIds = listOf(idB),
            ),
        )

    assertEquals(1, result.deletedCount)
    assertEquals(0, result.failedCount)
    assertNull(writer.loadDuplicateGroupMemberIds(groupId))
    assertNull(writer.loadFileEntryDeleteTarget(idB))
    assertNotNull(writer.loadFileEntryDeleteTarget(idA))
  }

  @Test
  fun runDelete_rejectsKeeperInDeleteList() {
    val coordinator =
        DeleteCoordinator(
            indexWriter = writer,
            uriValidator = UriValidator(context),
            platformFileDeleter = FakePlatformFileDeleter(),
        )
    try {
      coordinator.runDelete(
          DeleteDuplicatesCommand(
              groupId = 1L,
              keeperFileEntryId = 10L,
              deleteFileEntryIds = listOf(10L),
          ),
      )
      throw AssertionError("expected IllegalArgumentException")
    } catch (_: IllegalArgumentException) {
      // expected
    }
  }

  @Test
  fun deleteDuplicates_pendingPlatformDeleter_reportsFailures() {
    val uriA =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fphotos%2Fa.jpg",
        )
    val uriB =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fphotos%2Fb.jpg",
        )
    val idA = upsertHashedEntry(staged(uriA), "hash-dup", generation = 1)
    val idB = upsertHashedEntry(staged(uriB), "hash-dup", generation = 1)
    grouper.rebuildDuplicateGroups()
    val groupId = grouper.firstGroupId()!!

    val coordinator =
        DeleteCoordinator(
            indexWriter = writer,
            uriValidator = UriValidator(context),
            platformFileDeleter = PendingPlatformFileDeleter(),
        )
    val latch = CountDownLatch(1)
    var asyncResult: DeleteDuplicatesResult? = null
    coordinator.deleteDuplicates(
        DeleteDuplicatesCommand(
            groupId = groupId,
            keeperFileEntryId = idA,
            deleteFileEntryIds = listOf(idB),
        ),
    ) { result ->
      asyncResult = result
      latch.countDown()
    }
    assertTrue(latch.await(2, TimeUnit.SECONDS))
    assertNotNull(asyncResult)
    assertEquals(0, asyncResult!!.deletedCount)
    assertEquals(1, asyncResult!!.failedCount)
    assertNotNull(writer.loadDuplicateGroupMemberIds(groupId))
  }

  private fun Grouper.firstGroupId(): Long? {
    val cursor = database.readable().rawQuery("SELECT id FROM duplicate_group LIMIT 1", null)
    cursor.use {
      return if (it.moveToFirst()) it.getLong(0) else null
    }
  }

  private class FakePlatformFileDeleter : PlatformFileDeleter {
    override fun delete(uri: Uri, grant: ScanRootGrant): Boolean = true
  }

  private fun staged(uri: Uri): StagedFile {
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "photo-${uri.lastPathSegment}",
            mediaTypeHint = MediaTypeHint.IMAGE,
            sizeBytes = 200L,
            mtimeNs = 2_000_000_000L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = 200L,
        mtimeNs = entry.mtimeNs,
        inode = null,
        deviceId = null,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.IMAGE,
    )
  }

  private fun upsertHashedEntry(staged: StagedFile, hash: String, generation: Int): Long =
      writer.upsertHashed(
          HashedFile(staged, hash, NormalizationProfile.RAW_BYTES),
          generation,
      )
}
