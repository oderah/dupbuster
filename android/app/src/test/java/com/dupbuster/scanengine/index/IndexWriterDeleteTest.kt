package com.dupbuster.scanengine.index

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.stat.StagedFile
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
class IndexWriterDeleteTest {

  private lateinit var database: CatalogDatabase
  private lateinit var writer: IndexWriter
  private lateinit var grouper: Grouper
  private var rootId: Long = 0

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = CatalogDatabase.inMemory(context)
    database.writable()
    writer = IndexWriter(database)
    grouper = Grouper(database)
    rootId =
        writer.insertScanRoot(
            uriOrGrant = "content://test/tree",
            mode = ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun applyDuplicateDelete_removesGroupWhenOnlyKeeperRemains() {
    val uriA = Uri.parse("content://test/a.jpg")
    val uriB = Uri.parse("content://test/b.jpg")
    val idA = writer.upsertHashed(staged(uriA), generation = 1)
    val idB = writer.upsertHashed(staged(uriB), generation = 1)
    grouper.rebuildDuplicateGroups()
    val groupId = firstGroupId()

    writer.applyDuplicateDelete(
        groupId = groupId,
        keeperFileEntryId = idA,
        deletedFileEntryIds = listOf(idB),
    )

    assertNull(writer.loadDuplicateGroupMemberIds(groupId))
    assertNotNull(writer.loadFileEntryDeleteTarget(idA))
    assertNull(writer.loadFileEntryDeleteTarget(idB))
  }

  @Test
  fun loadFileEntryDeleteTarget_resolvesGrantFromScanRoot() {
    val id =
        writer.upsertHashed(
            staged(Uri.parse("content://test/only.jpg")),
            generation = 1,
        )
    val target = writer.loadFileEntryDeleteTarget(id)
    assertNotNull(target)
    assertEquals("content://test/tree", target!!.grant.uriGrant.toString())
    assertEquals(ScanRootMode.USER_SELECTED, target.grant.mode)
  }

  private fun firstGroupId(): Long {
    database.readable().rawQuery("SELECT id FROM duplicate_group LIMIT 1", null).use {
      assertTrue(it.moveToFirst())
      return it.getLong(0)
    }
  }

  private fun staged(uri: Uri): StagedFile {
    val entry =
        DiscoveredEntry(
            contentUri = uri,
            scanRootId = rootId,
            generation = 1,
            displayName = "x",
            mediaTypeHint = MediaTypeHint.IMAGE,
            sizeBytes = 100L,
            mtimeNs = 1L,
        )
    return StagedFile(
        discovered = entry,
        sizeBytes = 100L,
        mtimeNs = 1L,
        inode = null,
        deviceId = null,
        isSymlink = false,
        mediaTypeHint = MediaTypeHint.IMAGE,
    )
  }

  private fun IndexWriter.upsertHashed(staged: StagedFile, generation: Int): Long =
      upsertHashed(HashedFile(staged, "h1", NormalizationProfile.RAW_BYTES), generation)
}
