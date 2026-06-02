package com.dupbuster.scanengine.delete

import android.net.Uri
import com.dupbuster.scanengine.index.FileEntryDeleteTarget
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.security.UriProvenance
import com.dupbuster.scanengine.security.UriValidationResult
import com.dupbuster.scanengine.security.UriValidator
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Isolated delete phase (architecture §3 / §8) — separate executor from [ScanOrchestrator] hash pool.
 */
class DeleteCoordinator(
    private val indexWriter: IndexWriter,
    private val uriValidator: UriValidator,
    private val platformFileDeleter: PlatformFileDeleter,
    private val executor: Executor = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "dupbuster-delete").apply { isDaemon = true }
    },
) {

  fun deleteDuplicates(
      command: DeleteDuplicatesCommand,
      callback: (DeleteDuplicatesResult) -> Unit,
  ) {
    executor.execute {
      callback(runDelete(command))
    }
  }

  internal fun runDelete(command: DeleteDuplicatesCommand): DeleteDuplicatesResult {
    validateCommand(command)

    val validatedTargets = mutableListOf<FileEntryDeleteTarget>()
    var failedCount = 0

    for (fileEntryId in command.deleteFileEntryIds) {
      val target = indexWriter.loadFileEntryDeleteTarget(fileEntryId)
      if (target == null) {
        failedCount++
        continue
      }

      val uri = Uri.parse(target.uriOrPath)
      when (
          uriValidator.validate(
              candidate = uri,
              grant = target.grant,
              provenance = UriProvenance.DISCOVERY,
          )
      ) {
        is UriValidationResult.Denied -> failedCount++
        UriValidationResult.Allowed -> validatedTargets.add(target)
      }
    }

    val platformDeletedIds = platformFileDeleter.deleteTargets(validatedTargets).toSet()
    failedCount += validatedTargets.count { it.fileEntryId !in platformDeletedIds }

    if (platformDeletedIds.isNotEmpty()) {
      indexWriter.applyDuplicateDelete(
          groupId = command.groupId,
          keeperFileEntryId = command.keeperFileEntryId,
          deletedFileEntryIds = platformDeletedIds.toList(),
      )
    }

    return DeleteDuplicatesResult(
        deletedCount = platformDeletedIds.size,
        failedCount = failedCount,
    )
  }

  private fun validateCommand(command: DeleteDuplicatesCommand) {
    val members =
        indexWriter.loadDuplicateGroupMemberIds(command.groupId)
            ?: throw IllegalArgumentException("Unknown duplicate group: ${command.groupId}")

    if (command.keeperFileEntryId !in members) {
      throw IllegalArgumentException("keeper is not a member of group ${command.groupId}")
    }

    for (fileEntryId in command.deleteFileEntryIds) {
      if (fileEntryId !in members) {
        throw IllegalArgumentException("fileEntryId $fileEntryId is not in group ${command.groupId}")
      }
    }
  }
}
