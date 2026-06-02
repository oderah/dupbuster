package com.dupbuster.scanengine.delete

import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteDuplicatesCommandParserTest {

  @Test
  fun parse_mapsBridgePayload() {
    val command =
        DeleteDuplicatesCommandParser.parse(
            JavaOnlyMap().apply {
              putDouble("groupId", 3.0)
              putDouble("keeperFileEntryId", 10.0)
              putArray(
                  "deleteFileEntryIds",
                  JavaOnlyArray().apply {
                    pushDouble(20.0)
                    pushDouble(21.0)
                  },
              )
            },
        )
    assertEquals(3L, command.groupId)
    assertEquals(10L, command.keeperFileEntryId)
    assertEquals(listOf(20L, 21L), command.deleteFileEntryIds)
  }
}
