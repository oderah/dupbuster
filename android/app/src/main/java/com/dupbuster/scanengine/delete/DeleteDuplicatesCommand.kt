package com.dupbuster.scanengine.delete

/** JS → native delete payload (architecture §11.3). */
data class DeleteDuplicatesCommand(
    val groupId: Long,
    val keeperFileEntryId: Long,
    val deleteFileEntryIds: List<Long>,
)

data class DeleteDuplicatesResult(
    val deletedCount: Int,
    val failedCount: Int,
)
