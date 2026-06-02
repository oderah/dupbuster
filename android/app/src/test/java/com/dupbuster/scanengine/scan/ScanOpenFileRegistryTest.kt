package com.dupbuster.scanengine.scan

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class ScanOpenFileRegistryTest {

  @Test
  fun closeAll_closesRegisteredHandles() {
    val registry = ScanOpenFileRegistry()
    val closed = AtomicBoolean(false)
    val handle = AutoCloseable { closed.set(true) }
    registry.register(handle).close()
    assertTrue(closed.get())
  }

  @Test
  fun closeAll_closesStillOpenHandles() {
    val registry = ScanOpenFileRegistry()
    val closed = AtomicBoolean(false)
    registry.register(AutoCloseable { closed.set(true) })
    registry.closeAll()
    assertTrue(closed.get())
  }
}
