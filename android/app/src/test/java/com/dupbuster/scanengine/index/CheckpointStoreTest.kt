package com.dupbuster.scanengine.index

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dupbuster.scanengine.security.ScanRootMode
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
class CheckpointStoreTest {

  private lateinit var database: CatalogDatabase
  private lateinit var indexWriter: IndexWriter
  private lateinit var checkpoints: CheckpointStore
  private var rootId: Long = 0

  @Before
  fun setUp() {
    val context: Context = ApplicationProvider.getApplicationContext()
    database = CatalogDatabase.inMemory(context)
    database.writable()
    indexWriter = IndexWriter(database)
    checkpoints = CheckpointStore(database)
    rootId =
        indexWriter.insertScanRoot(
            uriOrGrant = "content://test/tree/docs",
            mode = ScanRootMode.USER_SELECTED,
        )
  }

  @Test
  fun beginRun_createsRunningRunWithZeroCheckpoint() {
    val runId = checkpoints.beginRun(rootId = rootId, generation = 1)

    val snapshot = checkpoints.getRun(runId)
    assertNotNull(snapshot)
    assertEquals(ScanRunStatus.RUNNING, snapshot!!.status)
    assertEquals(0L, snapshot.lastProcessedId)
    assertEquals(1, snapshot.generation)
  }

  @Test
  fun saveCheckpoint_persistsMonotonicLastProcessedId() {
    val runId = checkpoints.beginRun(rootId = rootId, generation = 1)

    checkpoints.saveCheckpoint(runId, lastProcessedId = 42)
    checkpoints.saveCheckpoint(runId, lastProcessedId = 100)

    assertEquals(100L, checkpoints.getRun(runId)!!.lastProcessedId)
  }

  @Test
  fun findResumableRun_returnsLatestRunningOrPaused_afterProcessKill() {
    val runId = checkpoints.beginRun(rootId = rootId, generation = 1)
    checkpoints.saveCheckpoint(runId, lastProcessedId = 500)
    // Simulated kill: status stays `running`, no ended_at.

    val resumable = checkpoints.findResumableRun()

    assertNotNull(resumable)
    assertEquals(runId, resumable!!.id)
    assertEquals(500L, resumable.lastProcessedId)
    assertEquals(ScanRunStatus.RUNNING, resumable.status)
  }

  @Test
  fun pauseAndResumeRun_restoresRunningStatusWithCheckpoint() {
    val runId = checkpoints.beginRun(rootId = rootId, generation = 1)
    checkpoints.saveCheckpoint(runId, lastProcessedId = 77)
    checkpoints.pauseRun(runId)

    assertEquals(ScanRunStatus.PAUSED, checkpoints.getRun(runId)!!.status)

    val resumed = checkpoints.resumeRun(runId)

    assertEquals(ScanRunStatus.RUNNING, resumed.status)
    assertEquals(77L, resumed.lastProcessedId)
  }

  @Test
  fun beginRun_throwsWhenConflictingActiveRunExists() {
    checkpoints.beginRun(rootId = rootId, generation = 1)

    try {
      checkpoints.beginRun(rootId = rootId, generation = 1)
      throw AssertionError("Expected CheckpointConflictException")
    } catch (expected: CheckpointConflictException) {
      assertEquals(rootId, expected.rootId)
      assertEquals(1, expected.generation)
    }
  }

  @Test
  fun beginRun_allowsNewGenerationAfterPriorRunComplete() {
    val first = checkpoints.beginRun(rootId = rootId, generation = 1)
    checkpoints.markComplete(first)
    assertNull(checkpoints.findResumableRun())

    val second = checkpoints.beginRun(rootId = rootId, generation = 2)

    assertTrue(second > first)
    assertEquals(second, checkpoints.findResumableRun()!!.id)
  }

  @Test
  fun abandonForRestart_makesRunNotResumable() {
    val runId = checkpoints.beginRun(rootId = rootId, generation = 1)
    checkpoints.saveCheckpoint(runId, lastProcessedId = 10)
    checkpoints.abandonForRestart(runId)

    assertEquals(ScanRunStatus.CANCELLED, checkpoints.getRun(runId)!!.status)
    assertNull(checkpoints.findResumableRun())
  }

  @Test(expected = IllegalArgumentException::class)
  fun saveCheckpoint_rejectsDecreasingLastProcessedId() {
    val runId = checkpoints.beginRun(rootId = rootId, generation = 1)
    checkpoints.saveCheckpoint(runId, lastProcessedId = 50)
    checkpoints.saveCheckpoint(runId, lastProcessedId = 10)
  }
}
