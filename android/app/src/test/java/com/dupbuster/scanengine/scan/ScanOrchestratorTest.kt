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
import com.dupbuster.scanengine.hash.HashedFile
import com.dupbuster.scanengine.hash.NormalizationProfile
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
import com.dupbuster.scanengine.stat.FileStat
import com.dupbuster.scanengine.stat.FileStatReadOutcome
import com.dupbuster.scanengine.stat.FileStatReader
import com.dupbuster.scanengine.stat.StatResult
import com.dupbuster.scanengine.stat.StagedFile
import com.dupbuster.scanengine.stat.ToctouStatVerifier
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
  private var latestStaged: StagedFile? = null

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
            toctouVerifier = echoToctouVerifier(),
            executor = syncExecutor,
        )
  }

  @Test
  fun startScan_toctouMismatchAfterHash_restatsAndIndexesFreshMetadata() {
    var statCalls = 0
    var verifyReads = 0
    val payload = "stable-payload".toByteArray(Charsets.UTF_8)

    val toctouOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner =
                ScanDiscoveryRunner { _, _, generation, consumer, _ ->
                  consumer.onEntry(discoveredEntry(1, generation))
                  DiscoveryResult(1, 0, 0, cancelled = false)
                },
            statFile = { entry, _ ->
              statCalls++
              val size = if (statCalls == 1) 4L else payload.size.toLong()
              val mtime = if (statCalls == 1) 100L else 200L
              StatResult.Success(staged(entry, size, mtime).also { latestStaged = it })
            },
            hashPipelineFactory = { _ ->
              HashPipeline(
                  context,
                  FakeContentReader(payload),
                  sizeBucketIndex = alwaysNeedsHashIndex(),
              )
            },
            progressBridge =
                ScanProgressBridge(
                    emitProgress = { map ->
                      emittedPhases.add(map.getString("phase")!!)
                    },
                ),
            emitError = {},
            toctouVerifier =
                ToctouStatVerifier(
                    object : FileStatReader {
                      override fun readStat(uri: Uri): FileStatReadOutcome {
                        verifyReads++
                        return when (verifyReads) {
                          1 ->
                              FileStatReadOutcome.Ok(
                                  FileStat(4, 100, null, null, false),
                              )
                          2 ->
                              FileStatReadOutcome.Ok(
                                  FileStat(payload.size.toLong(), 200, null, null, false),
                              )
                          else ->
                              FileStatReadOutcome.Ok(
                                  FileStat(
                                      latestStaged!!.sizeBytes,
                                      latestStaged!!.mtimeNs,
                                      null,
                                      null,
                                      false,
                                  ),
                              )
                        }
                      }
                    },
                ),
            executor = java.util.concurrent.Executor { it.run() },
        )

    toctouOrchestrator.startScan(
        ScanStartRequest(mode = ScanRootMode.PLATFORM_DISCOVERY, roots = emptyList()),
    )

    assertEquals(2, statCalls)
    assertTrue(verifyReads >= 3)
    database.readable().rawQuery(
        "SELECT size, mtime_ns FROM file_entry ORDER BY id DESC LIMIT 1",
        null,
    ).use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals(payload.size.toLong(), cursor.getLong(0))
      assertEquals(200L, cursor.getLong(1))
    }
  }

  @Test
  fun startScan_fileDeletedMidHash_tombstonesAndCompletesWithoutError() {
    val payload = "gone-mid-hash".toByteArray(Charsets.UTF_8)
    val entryUri = Uri.parse("$contentUri/deleted")
    var verifyReads = 0
    val emittedErrors = mutableListOf<String>()
    val platformRootId =
        indexWriter.findOrInsertScanRoot(
            PlatformDiscoveryGrant.MARKER_URI.toString(),
            ScanRootMode.PLATFORM_DISCOVERY,
        )

    indexWriter.upsertHashed(
        HashedFile(
            staged(
                DiscoveredEntry(
                    contentUri = entryUri,
                    scanRootId = platformRootId,
                    generation = 1,
                    displayName = "deleted.bin",
                    mediaTypeHint = MediaTypeHint.OTHER,
                    sizeBytes = payload.size.toLong(),
                    mtimeNs = 100L,
                ),
                payload.size.toLong(),
                100L,
            ),
            "prior-hash",
            NormalizationProfile.RAW_BYTES,
        ),
        generation = 1,
    )
    val priorRunId = indexWriter.beginScanRun(generation = 1, rootId = platformRootId)
    indexWriter.completeScanRun(priorRunId)

    val tombstoneOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner =
                ScanDiscoveryRunner { _, _, generation, consumer, _ ->
                  consumer.onEntry(
                      DiscoveredEntry(
                          contentUri = entryUri,
                          scanRootId = platformRootId,
                          generation = generation,
                          displayName = "deleted.bin",
                          mediaTypeHint = MediaTypeHint.OTHER,
                          sizeBytes = payload.size.toLong(),
                          mtimeNs = 100L,
                      ),
                  )
                  DiscoveryResult(1, 0, 0, cancelled = false)
                },
            statFile = { entry, _ ->
              StatResult.Success(staged(entry, payload.size.toLong(), 100L))
            },
            hashPipelineFactory = { _ ->
              HashPipeline(
                  context,
                  FakeContentReader(payload),
                  sizeBucketIndex = alwaysNeedsHashIndex(),
              )
            },
            progressBridge =
                ScanProgressBridge(
                    emitProgress = { map ->
                      emittedPhases.add(map.getString("phase")!!)
                    },
                ),
            emitError = { map -> emittedErrors.add(map.getString("unscannableReason")!!) },
            toctouVerifier =
                ToctouStatVerifier(
                    object : FileStatReader {
                      override fun readStat(uri: Uri): FileStatReadOutcome {
                        verifyReads++
                        val staged = latestStaged ?: return FileStatReadOutcome.IoFailure
                        if (verifyReads == 1) {
                          return FileStatReadOutcome.Ok(
                              FileStat(
                                  staged.sizeBytes,
                                  staged.mtimeNs,
                                  staged.inode,
                                  staged.deviceId,
                                  staged.isSymlink,
                              ),
                          )
                        }
                        return FileStatReadOutcome.IoFailure
                      }
                    },
                ),
            executor = java.util.concurrent.Executor { it.run() },
        )

    tombstoneOrchestrator.startScan(
        ScanStartRequest(mode = ScanRootMode.PLATFORM_DISCOVERY, roots = emptyList()),
    )

    assertTrue(emittedPhases.last() == ScanPhase.COMPLETE)
    assertTrue(emittedErrors.isEmpty())
    assertEquals(0, indexWriter.fileEntryCount())
  }

  @Test
  fun startScan_grantRevokedMidScan_pausesAndRetainsPartialResults() {
    val payload = "keep-me".toByteArray(Charsets.UTF_8)
    val platformRootId =
        indexWriter.findOrInsertScanRoot(
            PlatformDiscoveryGrant.MARKER_URI.toString(),
            ScanRootMode.PLATFORM_DISCOVERY,
        )
    val uriOk = Uri.parse("$contentUri/ok")
    val uriRevoked = Uri.parse("$contentUri/revoked")
    val emittedErrors = mutableListOf<String>()
    val pausedLatch = CountDownLatch(1)

    val revokeOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner =
                ScanDiscoveryRunner { _, _, generation, consumer, _ ->
                  consumer.onEntry(
                      DiscoveredEntry(
                          contentUri = uriOk,
                          scanRootId = platformRootId,
                          generation = generation,
                          displayName = "ok.bin",
                          mediaTypeHint = MediaTypeHint.OTHER,
                          sizeBytes = payload.size.toLong(),
                          mtimeNs = 1L,
                      ),
                  )
                  consumer.onEntry(
                      DiscoveredEntry(
                          contentUri = uriRevoked,
                          scanRootId = platformRootId,
                          generation = generation,
                          displayName = "revoked.bin",
                          mediaTypeHint = MediaTypeHint.OTHER,
                          sizeBytes = payload.size.toLong(),
                          mtimeNs = 2L,
                      ),
                  )
                  DiscoveryResult(2, 0, 0, cancelled = false)
                },
            statFile = { entry, _ ->
              if (entry.contentUri == uriOk) {
                StatResult.Success(staged(entry, payload.size.toLong(), 1L))
              } else {
                StatResult.Unscannable.permissionDenied()
              }
            },
            hashPipelineFactory = { _ ->
              HashPipeline(
                  context,
                  FakeContentReader(payload),
                  sizeBucketIndex = alwaysNeedsHashIndex(),
              )
            },
            progressBridge =
                ScanProgressBridge(
                    emitProgress = { map ->
                      emittedPhases.add(map.getString("phase")!!)
                      if (map.getString("phase") == ScanPhase.PAUSED) {
                        pausedLatch.countDown()
                      }
                    },
                ),
            emitError = { map -> emittedErrors.add(map.getString("unscannableReason")!!) },
            toctouVerifier = echoToctouVerifier(),
            executor = Executors.newSingleThreadExecutor(),
        )

    val scanRunId =
        revokeOrchestrator.startScan(
            ScanStartRequest(mode = ScanRootMode.PLATFORM_DISCOVERY, roots = emptyList()),
        )
    assertTrue(pausedLatch.await(2, TimeUnit.SECONDS))
    assertTrue(emittedPhases.contains(ScanPhase.PAUSED))
    assertTrue(emittedErrors.contains("PERMISSION_DENIED"))
    assertEquals(ScanRunStatus.PAUSED, checkpointStore.getRun(scanRunId)!!.status)
    assertEquals(1, indexWriter.fileEntryCount())

    revokeOrchestrator.cancelScan(scanRunId)
    Thread.sleep(200)
  }

  @Test
  fun getResumableScanRun_returnsLatestInterruptedRun() {
    val runId = checkpointStore.beginRun(rootId = null, generation = 1)
    checkpointStore.saveCheckpoint(runId, 100L)

    val resumable = orchestrator.getResumableScanRun()

    assertNotNull(resumable)
    assertEquals(runId, resumable!!.scanRunId)
    assertEquals(100L, resumable.lastProcessedId)
    assertEquals(ScanRunStatus.RUNNING, resumable.status)
  }

  @Test
  fun startScan_resumeFromCheckpoint_skipsAlreadyIndexedEntries() {
    val payload = "resume-payload".toByteArray(Charsets.UTF_8)
    val platformRootId =
        indexWriter.findOrInsertScanRoot(
            PlatformDiscoveryGrant.MARKER_URI.toString(),
            ScanRootMode.PLATFORM_DISCOVERY,
        )
    val uriFirst = Uri.parse("$contentUri/first-resume")
    val uriSecond = Uri.parse("$contentUri/second-resume")
    var statInvocations = 0

    var discoveryPass = 0
    val discoveryRunner =
        ScanDiscoveryRunner { _, _, generation, consumer, _ ->
          discoveryPass++
          consumer.onEntry(
              DiscoveredEntry(
                  contentUri = uriFirst,
                  scanRootId = platformRootId,
                  generation = generation,
                  displayName = "first.bin",
                  mediaTypeHint = MediaTypeHint.OTHER,
                  sizeBytes = payload.size.toLong(),
                  mtimeNs = 1L,
              ),
          )
          if (discoveryPass > 1) {
            consumer.onEntry(
                DiscoveredEntry(
                    contentUri = uriSecond,
                    scanRootId = platformRootId,
                    generation = generation,
                    displayName = "second.bin",
                    mediaTypeHint = MediaTypeHint.OTHER,
                    sizeBytes = payload.size.toLong(),
                    mtimeNs = 2L,
                ),
            )
          }
          DiscoveryResult(if (discoveryPass > 1) 2 else 1, 0, 0, cancelled = false)
        }

    val statFile: (DiscoveredEntry, ScanRootGrant) -> StatResult = { entry, _ ->
      statInvocations++
      StatResult.Success(staged(entry, payload.size.toLong(), entry.mtimeNs))
    }

    val hashFactory = { writer: IndexWriter ->
      HashPipeline(
          context,
          FakeContentReader(payload),
          sizeBucketIndex = alwaysNeedsHashIndex(),
      )
    }

    val firstOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner = discoveryRunner,
            statFile = statFile,
            hashPipelineFactory = hashFactory,
            progressBridge = ScanProgressBridge(emitProgress = {}),
            emitError = {},
            toctouVerifier = echoToctouVerifier(),
            executor = java.util.concurrent.Executor { it.run() },
        )

    val runId =
        firstOrchestrator.startScan(
            ScanStartRequest(mode = ScanRootMode.PLATFORM_DISCOVERY, roots = emptyList()),
        )
    val interrupted = checkpointStore.getRun(runId)!!
    assertEquals(ScanRunStatus.COMPLETE, interrupted.status)
    assertEquals(1, statInvocations)

    statInvocations = 0
    val resumeOrchestrator =
        ScanOrchestrator(
            indexWriter = indexWriter,
            checkpointStore = checkpointStore,
            grouper = Grouper(database),
            discoveryRunner = discoveryRunner,
            statFile = statFile,
            hashPipelineFactory = hashFactory,
            progressBridge =
                ScanProgressBridge(
                    emitProgress = { map ->
                      emittedPhases.add(map.getString("phase")!!)
                    },
                ),
            emitError = {},
            toctouVerifier = echoToctouVerifier(),
            executor = java.util.concurrent.Executor { it.run() },
        )

    checkpointStore.getRun(runId)!!.let { run ->
      database.writable().execSQL(
          "UPDATE scan_run SET status = '${ScanRunStatus.RUNNING}', ended_at = NULL WHERE id = ${run.id}",
      )
    }

    resumeOrchestrator.startScan(
        ScanStartRequest(
            mode = ScanRootMode.PLATFORM_DISCOVERY,
            roots = emptyList(),
            resumeScanRunId = runId,
        ),
    )

    assertEquals(1, statInvocations)
    assertTrue(emittedPhases.contains(ScanPhase.COMPLETE))
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
            toctouVerifier = echoToctouVerifier(),
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
            toctouVerifier = echoToctouVerifier(),
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

  private fun staged(entry: DiscoveredEntry, sizeBytes: Long, mtimeNs: Long = entry.mtimeNs): StagedFile =
      StagedFile(
          discovered = entry,
          sizeBytes = sizeBytes,
          mtimeNs = mtimeNs,
          inode = mtimeNs,
          deviceId = 1L,
          isSymlink = false,
          mediaTypeHint = entry.mediaTypeHint,
      ).also { latestStaged = it }

  private fun echoToctouVerifier(): ToctouStatVerifier =
      ToctouStatVerifier(
          object : FileStatReader {
            override fun readStat(uri: Uri): FileStatReadOutcome {
              val staged = latestStaged ?: return FileStatReadOutcome.IoFailure
              return FileStatReadOutcome.Ok(
                  FileStat(
                      sizeBytes = staged.sizeBytes,
                      mtimeNs = staged.mtimeNs,
                      inode = staged.inode,
                      deviceId = staged.deviceId,
                      isSymlink = staged.isSymlink,
                  ),
              )
            }
          },
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
