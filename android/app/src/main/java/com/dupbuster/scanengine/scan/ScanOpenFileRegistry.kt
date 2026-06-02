package com.dupbuster.scanengine.scan

import java.util.concurrent.ConcurrentHashMap

/** Tracks open read FDs so grant revocation can close in-flight handles (architecture §6.4). */
class ScanOpenFileRegistry {
  private val openHandles = ConcurrentHashMap.newKeySet<AutoCloseable>()

  fun register(handle: AutoCloseable): AutoCloseable {
    openHandles.add(handle)
    return AutoCloseable {
      openHandles.remove(handle)
      runCatching { handle.close() }
    }
  }

  fun closeAll() {
    openHandles.forEach { handle ->
      runCatching { handle.close() }
    }
    openHandles.clear()
  }
}
