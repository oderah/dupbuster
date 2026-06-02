package com.dupbuster.scanengine.scan

import com.dupbuster.scanengine.bridge.ScanErrorBridgeMapper
import com.dupbuster.scanengine.bridge.ScanPhase
import com.dupbuster.scanengine.bridge.ScanProgressBridge
import com.dupbuster.scanengine.bridge.ScanProgressSnapshot
import com.dupbuster.scanengine.discovery.DiscoveredEntry
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.HashPipeline
import com.dupbuster.scanengine.hash.HashResult
import com.dupbuster.scanengine.hash.HashSettings
import com.dupbuster.scanengine.index.CheckpointConflictException
import com.dupbuster.scanengine.index.CheckpointStore
import com.dupbuster.scanengine.index.Grouper
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.index.ScanRunStatus
import com.dupbuster.scanengine.security.ScanRootGrant
import com.dupbuster.scanengine.stat.StatResult
import com.dupbuster.scanengine.stat.StagedFile
import com.dupbuster.scanengine.stat.ToctouStatVerifier
import com.dupbuster.scanengine.stat.ToctouVerifyOutcome
import com.dupbuster.scanengine.security.UnscannableReason
import com.facebook.react.bridge.ReadableMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Wires discovery → stat → hash → index → group with throttled progress (M1-19).
 * Runs off the JS thread; bridge payloads remain metadata-only.
 */
class ScanOrchestrator(
    private val indexWriter: IndexWriter,
    private val checkpointStore: CheckpointStore,
    private val grouper: Grouper,
    private val discoveryRunner: ScanDiscoveryRunner,
    private val statFile: (DiscoveredEntry, ScanRootGrant) -> StatResult,
    private val hashPipelineFactory: (IndexWriter) -> HashPipeline,
    private val progressBridge: ScanProgressBridge,
    private val emitError: (ReadableMap) -> Unit,
    private val toctouVerifier: ToctouStatVerifier,
    private val executor: Executor = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "dupbuster-scan-orchestrator").apply { isDaemon = true }
    },
    private val clock: () -> Long = System::currentTimeMillis,
) {
  private data class ActiveSession(
      val scanRunId: Long,
      val control: ScanSessionControl,
  )

  private val activeSession = AtomicReference<ActiveSession?>(null)

  @Throws(IllegalStateException::class, CheckpointConflictException::class, IllegalArgumentException::class)
  fun startScan(request: ScanStartRequest): Long {
    if (request.resumeScanRunId != null) {
      throw UnsupportedOperationException("resumeScanRunId is not supported until resume wiring lands")
    }
    assertNoConflictingActiveScan()

    val plan = ScanRootResolver.resolve(request, indexWriter)
    val generation = indexWriter.nextGenerationForRoot(plan.primaryRootId)
    val scanRunId =
        indexWriter.beginScanRun(
            generation = generation,
            rootId = plan.primaryRootId,
        )
    val control = ScanSessionControl()
    activeSession.set(ActiveSession(scanRunId = scanRunId, control = control))

    executor.execute {
      runScan(request, plan, generation, scanRunId, control)
    }

    return scanRunId
  }

  fun pauseScan(scanRunId: Long) {
    val session = requireActiveSession(scanRunId)
    session.control.paused = true
    checkpointStore.pauseRun(scanRunId)
    emitPhase(
        ScanProgressSnapshot(
            filesProcessed = 0,
            filesTotalKnown = null,
            groupsFound = 0,
            reclaimableBytesEst = 0,
            phase = ScanPhase.PAUSED,
        ),
    )
  }

  fun resumeScan(scanRunId: Long) {
    val session = requireActiveSession(scanRunId)
    session.control.paused = false
    checkpointStore.resumeRun(scanRunId)
    emitPhase(
        ScanProgressSnapshot(
            filesProcessed = 0,
            filesTotalKnown = null,
            groupsFound = 0,
            reclaimableBytesEst = 0,
            phase = ScanPhase.HASHING,
        ),
    )
  }

  fun cancelScan(scanRunId: Long) {
    val session = requireActiveSession(scanRunId)
    session.control.cancelRequested = true
    session.control.paused = false
    checkpointStore.markCancelling(scanRunId)
    emitPhase(
        ScanProgressSnapshot(
            filesProcessed = 0,
            filesTotalKnown = null,
            groupsFound = 0,
            reclaimableBytesEst = 0,
            phase = ScanPhase.CANCELLING,
        ),
    )
  }

  private fun assertNoConflictingActiveScan() {
    val current = activeSession.get() ?: return
    val run = checkpointStore.getRun(current.scanRunId) ?: return
    if (run.status in ScanRunStatus.ACTIVE) {
      throw IllegalStateException("A scan is already active")
    }
  }

  private fun requireActiveSession(scanRunId: Long): ActiveSession {
    val session =
        activeSession.get()
            ?: throw IllegalArgumentException("No active scan session")
    if (session.scanRunId != scanRunId) {
      throw IllegalArgumentException("scanRunId $scanRunId is not the active run")
    }
    return session
  }

  private fun runScan(
      request: ScanStartRequest,
      plan: ScanRootResolver.ResolvedPlan,
      generation: Int,
      scanRunId: Long,
      control: ScanSessionControl,
  ) {
    progressBridge.reset()
    try {
      emitPhase(
          ScanProgressSnapshot(
              filesProcessed = 0,
              filesTotalKnown = null,
              groupsFound = 0,
              reclaimableBytesEst = 0,
              phase = ScanPhase.DISCOVERING,
          ),
      )

      val entries = mutableListOf<DiscoveredEntry>()
      val discoveryResult =
          discoveryRunner.discover(
              request = request,
              plan = plan,
              generation = generation,
              consumer = { entry -> entries.add(entry) },
              isCancelled = control::isCancelled,
          )

      if (control.isCancelled() || discoveryResult.cancelled) {
        finishCancelled(scanRunId, filesProcessed = 0, filesTotalKnown = entries.size)
        return
      }

      val hashPipeline = hashPipelineFactory(indexWriter)
      val totalFiles = entries.size
      var filesProcessed = 0
      var groupsFound = 0
      var reclaimableBytesEst = 0L

      emitPhase(
          ScanProgressSnapshot(
              filesProcessed = 0,
              filesTotalKnown = totalFiles,
              groupsFound = 0,
              reclaimableBytesEst = 0,
              phase = ScanPhase.HASHING,
          ),
      )

      for (entry in entries) {
        control.awaitIfPaused()
        if (control.isCancelled()) {
          finishCancelled(scanRunId, filesProcessed, totalFiles)
          return
        }

        val grant = grantForEntry(entry, plan)
        val fileEntryId =
            when (val statResult = statFile(entry, grant)) {
              is StatResult.Success ->
                  processHashResult(
                      hashPipeline = hashPipeline,
                      entry = entry,
                      grant = grant,
                      initialStaged = statResult.staged,
                      generation = generation,
                      scanRunId = scanRunId,
                      plan = plan,
                  )
              is StatResult.Unscannable ->
                  indexWriter.upsertUnscannable(
                      statResult.reason,
                      stagedFromDiscovered(entry),
                      generation,
                  ).also { id ->
                    emitError(
                        ScanErrorBridgeMapper.toReadableMap(
                            fileEntryId = id,
                            unscannableReason = statResult.reason,
                            scanRunId = scanRunId,
                        ),
                    )
                  }
            }

        filesProcessed++
        indexWriter.updateScanRunCheckpoint(scanRunId, fileEntryId)
        reportHashingProgress(
            mediaTypeHint = entry.mediaTypeHint,
            filesProcessed = filesProcessed,
            filesTotalKnown = totalFiles,
            groupsFound = groupsFound,
            reclaimableBytesEst = reclaimableBytesEst,
        )
      }

      emitPhase(
          ScanProgressSnapshot(
              filesProcessed = filesProcessed,
              filesTotalKnown = totalFiles,
              groupsFound = groupsFound,
              reclaimableBytesEst = reclaimableBytesEst,
              phase = ScanPhase.GROUPING,
          ),
      )

      backfillImageContentFingerprints(
          hashPipeline = hashPipeline,
          generation = generation,
          scanRunId = scanRunId,
          plan = plan,
          control = control,
      )
      backfillVideoContentFingerprints(
          hashPipeline = hashPipeline,
          generation = generation,
          scanRunId = scanRunId,
          plan = plan,
          control = control,
      )

      val groupResult = grouper.rebuildDuplicateGroups()
      groupsFound = groupResult.groupsCreated
      reclaimableBytesEst = groupResult.totalReclaimableBytesEst

      indexWriter.purgeEntriesNotSeenInGeneration(plan.primaryRootId, generation)
      indexWriter.completeScanRun(scanRunId)

      emitPhase(
          ScanProgressSnapshot(
              filesProcessed = filesProcessed,
              filesTotalKnown = totalFiles,
              groupsFound = groupsFound,
              reclaimableBytesEst = reclaimableBytesEst,
              phase = ScanPhase.COMPLETE,
          ),
      )
    } catch (error: Exception) {
      checkpointStore.markError(scanRunId, teardownReason = error.javaClass.simpleName)
      emitPhase(
          ScanProgressSnapshot(
              filesProcessed = 0,
              filesTotalKnown = null,
              groupsFound = 0,
              reclaimableBytesEst = 0,
              phase = ScanPhase.ERROR,
          ),
      )
    } finally {
      activeSession.set(null)
    }
  }

  private fun processHashResult(
      hashPipeline: HashPipeline,
      entry: DiscoveredEntry,
      grant: ScanRootGrant,
      initialStaged: StagedFile,
      generation: Int,
      scanRunId: Long,
      plan: ScanRootResolver.ResolvedPlan,
  ): Long {
    var staged = initialStaged
    var mismatchAttempts = 0

    while (true) {
      when (val preCheck = toctouVerifier.verifyBaseline(staged)) {
        ToctouVerifyOutcome.Consistent -> Unit
        is ToctouVerifyOutcome.Changed -> {
          mismatchAttempts++
          if (mismatchAttempts > ToctouStatVerifier.MAX_MISMATCH_RETRIES) {
            return upsertToctouUnscannable(staged, generation, scanRunId)
          }
          staged = restatForToctou(entry, grant) ?: return upsertToctouUnscannable(staged, generation, scanRunId)
          continue
        }
        ToctouVerifyOutcome.IoFailure ->
            return upsertToctouUnscannable(staged, generation, scanRunId)
      }

      val hashResult = hashPipeline.hash(staged, HashSettings())

      when (val postCheck = toctouVerifier.verifyBaseline(staged)) {
        ToctouVerifyOutcome.Consistent ->
            return persistHashOutcome(
                hashPipeline = hashPipeline,
                hashResult = hashResult,
                staged = staged,
                generation = generation,
                scanRunId = scanRunId,
                plan = plan,
            )
        is ToctouVerifyOutcome.Changed -> {
          mismatchAttempts++
          if (mismatchAttempts > ToctouStatVerifier.MAX_MISMATCH_RETRIES) {
            return persistHashOutcome(
                hashPipeline = hashPipeline,
                hashResult = hashResult,
                staged = toctouVerifier.applyFreshStat(staged, postCheck.freshStat),
                generation = generation,
                scanRunId = scanRunId,
                plan = plan,
            )
          }
          staged = restatForToctou(entry, grant) ?: return upsertToctouUnscannable(staged, generation, scanRunId)
          continue
        }
        ToctouVerifyOutcome.IoFailure ->
            return upsertToctouUnscannable(staged, generation, scanRunId)
      }
    }
  }

  private fun restatForToctou(entry: DiscoveredEntry, grant: ScanRootGrant): StagedFile? =
      when (val statResult = statFile(entry, grant)) {
        is StatResult.Success -> statResult.staged
        is StatResult.Unscannable -> null
      }

  private fun upsertToctouUnscannable(
      staged: StagedFile,
      generation: Int,
      scanRunId: Long,
  ): Long {
    val fileEntryId =
        indexWriter.upsertUnscannable(
            UnscannableReason.PERMISSION_DENIED,
            staged,
            generation,
        )
    emitError(
        ScanErrorBridgeMapper.toReadableMap(
            fileEntryId = fileEntryId,
            unscannableReason = UnscannableReason.PERMISSION_DENIED,
            scanRunId = scanRunId,
        ),
    )
    return fileEntryId
  }

  private fun persistHashOutcome(
      hashPipeline: HashPipeline,
      hashResult: HashResult,
      staged: StagedFile,
      generation: Int,
      scanRunId: Long,
      plan: ScanRootResolver.ResolvedPlan,
  ): Long {
    val fileEntryId = indexWriter.persistHashResult(hashResult, staged, generation)
    if (hashResult is HashResult.Unscannable) {
      emitError(
          ScanErrorBridgeMapper.toReadableMap(
              fileEntryId = fileEntryId,
              unscannableReason = hashResult.reason,
              scanRunId = scanRunId,
          ),
      )
    }
    if (hashResult !is HashResult.SizeBucketSkipped) {
      backfillSizeBucketSkippedPeers(
          hashPipeline = hashPipeline,
          sizeBytes = staged.sizeBytes,
          generation = generation,
          scanRunId = scanRunId,
          plan = plan,
          excludeFileEntryId = fileEntryId,
      )
    }
    return fileEntryId
  }

  /**
   * When a size collision triggers hashing, re-hash prior size-bucket skips so grouping sees
   * every file at that size (FR-FP-02 backfill; fixes missed duplicate groups).
   */
  /**
   * Ensures every indexed image has `IMAGE_CONTENT_V1` before grouping (legacy size-bucket skips
   * and incremental catalog rows that only stored `RAW_BYTES`).
   */
  private fun backfillVideoContentFingerprints(
      hashPipeline: HashPipeline,
      generation: Int,
      scanRunId: Long,
      plan: ScanRootResolver.ResolvedPlan,
      control: ScanSessionControl,
  ) {
    for (pending in indexWriter.listVideoContentBackfillEntries(generation)) {
      control.awaitIfPaused()
      if (control.isCancelled()) {
        return
      }
      val entry = pending.toDiscoveredEntry()
      val grant = grantForEntry(entry, plan)
      when (val statResult = statFile(entry, grant)) {
        is StatResult.Success ->
            processHashResult(
                hashPipeline = hashPipeline,
                entry = entry,
                grant = grant,
                initialStaged = statResult.staged,
                generation = generation,
                scanRunId = scanRunId,
                plan = plan,
            )
        is StatResult.Unscannable ->
            indexWriter.upsertUnscannable(
                statResult.reason,
                stagedFromDiscovered(entry),
                generation,
            )
      }
    }
  }

  private fun backfillImageContentFingerprints(
      hashPipeline: HashPipeline,
      generation: Int,
      scanRunId: Long,
      plan: ScanRootResolver.ResolvedPlan,
      control: ScanSessionControl,
  ) {
    for (pending in indexWriter.listImageContentBackfillEntries(generation)) {
      control.awaitIfPaused()
      if (control.isCancelled()) {
        return
      }
      val entry = pending.toDiscoveredEntry()
      val grant = grantForEntry(entry, plan)
      when (val statResult = statFile(entry, grant)) {
        is StatResult.Success ->
            processHashResult(
                hashPipeline = hashPipeline,
                entry = entry,
                grant = grant,
                initialStaged = statResult.staged,
                generation = generation,
                scanRunId = scanRunId,
                plan = plan,
            )
        is StatResult.Unscannable ->
            indexWriter.upsertUnscannable(
                statResult.reason,
                stagedFromDiscovered(entry),
                generation,
            )
      }
    }
  }

  private fun backfillSizeBucketSkippedPeers(
      hashPipeline: HashPipeline,
      sizeBytes: Long,
      generation: Int,
      scanRunId: Long,
      plan: ScanRootResolver.ResolvedPlan,
      excludeFileEntryId: Long,
  ) {
    val pending =
        indexWriter.listSizeBucketPendingEntries(
            sizeBytes = sizeBytes,
            generation = generation,
            excludeFileEntryId = excludeFileEntryId,
        )
    for (peer in pending) {
      val entry = peer.toDiscoveredEntry()
      val grant = grantForEntry(entry, plan)
      when (val statResult = statFile(entry, grant)) {
        is StatResult.Success ->
            processHashResult(
                hashPipeline = hashPipeline,
                entry = entry,
                grant = grant,
                initialStaged = statResult.staged,
                generation = generation,
                scanRunId = scanRunId,
                plan = plan,
            )
        is StatResult.Unscannable ->
            indexWriter.upsertUnscannable(
                statResult.reason,
                stagedFromDiscovered(entry),
                generation,
            ).also { id ->
              emitError(
                  ScanErrorBridgeMapper.toReadableMap(
                      fileEntryId = id,
                      unscannableReason = statResult.reason,
                      scanRunId = scanRunId,
                  ),
              )
            }
      }
    }
  }

  private fun reportHashingProgress(
      mediaTypeHint: MediaTypeHint,
      filesProcessed: Int,
      filesTotalKnown: Int,
      groupsFound: Int,
      reclaimableBytesEst: Long,
  ) {
    progressBridge.reportHashingProgress(
        filesProcessed = filesProcessed,
        filesTotalKnown = filesTotalKnown,
        groupsFound = groupsFound,
        reclaimableBytesEst = reclaimableBytesEst,
        mediaTypeHint = mediaTypeHint,
        isVideoFingerprintPass = false,
        atMs = clock(),
    )
    progressBridge.advanceTo(clock())
  }

  private fun finishCancelled(scanRunId: Long, filesProcessed: Int, filesTotalKnown: Int) {
    checkpointStore.markCancelled(scanRunId)
    emitPhase(
        ScanProgressSnapshot(
            filesProcessed = filesProcessed,
            filesTotalKnown = filesTotalKnown,
            groupsFound = 0,
            reclaimableBytesEst = 0,
            phase = ScanPhase.CANCELLED,
        ),
    )
  }

  private fun grantForEntry(
      entry: DiscoveredEntry,
      plan: ScanRootResolver.ResolvedPlan,
  ): ScanRootGrant {
    val resolved =
        plan.roots.firstOrNull { it.scanRootId == entry.scanRootId }
            ?: plan.roots.first()
    return ScanRootGrant(
        uriGrant = android.net.Uri.parse(resolved.uriGrant),
        mode = resolved.mode,
    )
  }

  private fun emitPhase(snapshot: ScanProgressSnapshot) {
    progressBridge.report(snapshot, clock())
    progressBridge.flush(clock())
  }
}
