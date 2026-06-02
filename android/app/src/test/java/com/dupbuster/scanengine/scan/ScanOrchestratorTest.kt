package com.dupbuster.scanengine.scan

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.bridge.ScanPhase
import com.dupbuster.scanengine.bridge.ScanProgressBridge
import com.dupbuster.scanengine.bridge.ScanProgressSnapshot
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.DiscoveryEntryConsumer
import com.dupbuster.scanengine.discovery.DiscoveryResult
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.discovery.PlatformDiscoveryGrant
import com.dupbuster.scanengine.hash.HashPipeline
import com.dupbuster.scanengine.hash.ContentOpenOutcome
import com.dupbuster.scanengine.hash.FileContentReader
import com.dupbuster.scanengine.hash.SizeBucketDisposition
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.CheckpointStore
import com.dupbuster.scanengine.index.Grouper
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.index.ScanRunStatus
import com.dupbuster.scanengine.index.SqliteSizeBucketIndex
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.security.ScanRootMode
import com.dupbuster.scanengine.stat.StatResult
import com.dupbuster.scanengine.stat.StagedFile
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScanOrchestratorTest {

  private lateinit var context: Context
  private lateinit var database: CatalogDatabase
  private lateinit var indexWriter: IndexWriter
  private lateinit var checkpointStore: CheckpointStore
  private lateinit var emittedPhases: MutableList<String>
  private lateinit var orchestrator: ScanOrchestrator

  private val contentUri =
      Uri.parse(
          "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fpayload.bin",
      )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = CatalogDatabase.inMemory(context)
    database.writable()
    indexWriter = IndexWriter(database)
    checkpointStore = CheckpointStore(database)
    emittedPhases = mutableListOf()

    val syncExecutor = java.util.concurrent.Executor { command -> command.run() }
    val progressBridge =
        ScanProgressBridge(
            emitProgress = { map ->
              emittedPhases.add(map.getString("phase")!!)
            },
        )

    val payload = "duplicate-payload".toByteArray(Charsets.UTF_8)
    orchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner = fakeDiscoveryRunner(),
            statFile = { entry, _ -> StatResult.Success(staged(entry, payload.size.toLong())) },
            hashPipelineFactory = { _ ->
              HashPipeline(
                  context,
                  FakeContentReader(payload),
                  sizeBucketIndex = alwaysNeedsHashIndex(),
              )
            },
            progressBridge = progressBridge,
            emitError = {},
            executor = syncExecutor,
        )
  }

  @Test
  fun startScan_runsPipeline_andEmitsTerminalComplete() {
    val scanRunId =
        orchestrator.startScan(
            ScanStartRequest(
                mode = ScanRootMode.PLATFORM_DISCOVERY,
                roots = emptyList(),
            ),
        )

    val run = checkpointStore.getRun(scanRunId)
    assertNotNull(run)
    assertEquals(ScanRunStatus.COMPLETE, run!!.status)
    assertTrue(emittedPhases.contains(ScanPhase.DISCOVERING))
    assertTrue(emittedPhases.contains(ScanPhase.HASHING))
    assertTrue(emittedPhases.contains(ScanPhase.GROUPING))
    assertTrue(emittedPhases.last() == ScanPhase.COMPLETE)
    assertEquals(2, indexWriter.fileEntryCount())
    assertTrue(Grouper(database).rebuildDuplicateGroups().groupsCreated >= 1)
  }

  @Test
  fun startScan_sizeBucketCollision_backfillsSkippedPeerAndGroups() {
    val payload = "duplicate-payload".toByteArray(Charsets.UTF_8)
    val payloadSize = payload.size.toLong()
    val sizeBucketOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner = fakeDiscoveryRunner(),
            statFile = { entry, _ ->
              StatResult.Success(staged(entry, payloadSize))
            },
            hashPipelineFactory = { writer ->
              HashPipeline(
                  context,
                  FakeContentReader(payload),
                  sizeBucketIndex = SqliteSizeBucketIndex(writer),
              )
            },
            progressBridge =
                ScanProgressBridge(
                    emitProgress = { map ->
                      emittedPhases.add(map.getString("phase")!!)
                    },
                ),
            emitError = {},
            executor = java.util.concurrent.Executor { it.run() },
        )

    sizeBucketOrchestrator.startScan(
        ScanStartRequest(mode = ScanRootMode.PLATFORM_DISCOVERY, roots = emptyList()),
    )

    assertTrue(Grouper(database).rebuildDuplicateGroups().groupsCreated >= 1)
  }

  @Test
  fun cancelScan_marksRunCancelled() {
    val phases = mutableListOf<String>()

    val asyncOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner =
                ScanDiscoveryRunner { _, _, _, _, isCancelled ->
                  while (!isCancelled()) {
                    Thread.sleep(20)
                  }
                  DiscoveryResult(0, 0, 0, cancelled = true)
                },
            statFile = { entry, _ -> StatResult.Success(staged(entry, 5)) },
            hashPipelineFactory = { _ ->
              HashPipeline(
                  context,
                  FakeContentReader("hello".toByteArray()),
                  sizeBucketIndex = alwaysNeedsHashIndex(),
              )
            },
            progressBridge =
                ScanProgressBridge(
                    emitProgress = { map ->
                      phases.add(map.getString("phase")!!)
                    },
                ),
            emitError = {},
            executor = Executors.newSingleThreadExecutor(),
        )

    val scanRunId =
        asyncOrchestrator.startScan(
            ScanStartRequest(mode = ScanRootMode.PLATFORM_DISCOVERY, roots = emptyList()),
        )
    Thread.sleep(50)
    asyncOrchestrator.cancelScan(scanRunId)
    Thread.sleep(200)

    val run = checkpointStore.getRun(scanRunId)
    assertNotNull(run)
    assertEquals(ScanRunStatus.CANCELLED, run!!.status)
    assertTrue(phases.contains(ScanPhase.CANCELLING))
    assertTrue(phases.contains(ScanPhase.CANCELLED))
  }

  private fun fakeDiscoveryRunner(): ScanDiscoveryRunner =
      ScanDiscoveryRunner { _, _, generation, consumer, _ ->
        consumer.onEntry(discoveredEntry(1, generation))
        consumer.onEntry(discoveredEntry(2, generation))
        DiscoveryResult(entriesEmitted = 2, entriesDenied = 0, directoriesVisited = 0, cancelled = false)
      }

  private fun discoveredEntry(id: Long, generation: Int = 1): DiscoveredEntry =
      DiscoveredEntry(
          contentUri = Uri.parse("$contentUri/$id"),
          scanRootId = 1L,
          generation = generation,
          displayName = "file-$id.bin",
          mediaTypeHint = MediaTypeHint.OTHER,
          sizeBytes = 15,
          mtimeNs = id,
      )

  private fun staged(entry: DiscoveredEntry, sizeBytes: Long): StagedFile =
      StagedFile(
          discovered = entry,
          sizeBytes = sizeBytes,
          mtimeNs = entry.mtimeNs,
          inode = entry.mtimeNs,
          deviceId = 1L,
          isSymlink = false,
          mediaTypeHint = entry.mediaTypeHint,
      )

  private fun alwaysNeedsHashIndex() =
      object : com.dupbuster.scanengine.hash.SizeBucketIndex {
        override fun register(
            sizeBytes: Long,
            mediaTypeHint: MediaTypeHint,
            isEmpty: Boolean,
        ): com.dupbuster.scanengine.hash.SizeBucketDisposition =
            SizeBucketDisposition.NEEDS_HASH
      }

  private class FakeContentReader(private val payload: ByteArray) : FileContentReader {
    override fun openRead(uri: Uri): ContentOpenOutcome =
        ContentOpenOutcome.Ok(ByteArrayInputStream(payload))

    override fun readRange(uri: Uri, offset: Long, length: Int): ByteArray? {
      if (offset >= payload.size) {
        return ByteArray(0)
      }
      val end = minOf(offset + length, payload.size.toLong())
      return payload.copyOfRange(offset.toInt(), end.toInt())
    }
  }
}
