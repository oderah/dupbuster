package com.dupbuster.scanengine.delete

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap

object DeleteDuplicatesCommandParser {

  fun parse(command: ReadableMap): DeleteDuplicatesCommand {
    if (!command.hasKey("groupId") || command.isNull("groupId")) {
      throw IllegalArgumentException("deleteDuplicates requires groupId")
    }
    if (!command.hasKey("keeperFileEntryId") || command.isNull("keeperFileEntryId")) {
      throw IllegalArgumentException("deleteDuplicates requires keeperFileEntryId")
    }
    if (!command.hasKey("deleteFileEntryIds") || command.isNull("deleteFileEntryIds")) {
      throw IllegalArgumentException("deleteDuplicates requires deleteFileEntryIds")
    }

    val groupId = command.getDouble("groupId").toLong()
    val keeperFileEntryId = command.getDouble("keeperFileEntryId").toLong()
    val deleteIdsArray =
        command.getArray("deleteFileEntryIds")
            ?: throw IllegalArgumentException("deleteFileEntryIds must be an array")

    val deleteFileEntryIds = parseDeleteIds(deleteIdsArray)
    if (deleteFileEntryIds.isEmpty()) {
      throw IllegalArgumentException("deleteFileEntryIds must not be empty")
    }
    if (keeperFileEntryId in deleteFileEntryIds) {
      throw IllegalArgumentException("keeper must not appear in deleteFileEntryIds")
    }

    return DeleteDuplicatesCommand(
        groupId = groupId,
        keeperFileEntryId = keeperFileEntryId,
        deleteFileEntryIds = deleteFileEntryIds,
    )
  }

  private fun parseDeleteIds(array: ReadableArray): List<Long> {
    val ids = mutableListOf<Long>()
    for (index in 0 until array.size()) {
      if (array.isNull(index)) {
        throw IllegalArgumentException("deleteFileEntryIds[$index] must be a number")
      }
      ids.add(array.getDouble(index).toLong())
    }
    return ids.distinct()
  }
}
