package com.dupbuster.scanengine.fixtures

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** M1-18: load equiv-* fixture rows and assert native `expect` outcomes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EquivFixturesTest {

  @Test
  fun allEquivalenceFixtures_matchExpect() {
    val failures = mutableListOf<String>()
    for (path in FixtureLoader.equivalenceFixturePaths()) {
      try {
        EquivFixtureRunner.run(FixtureLoader.load(path))
      } catch (error: Throwable) {
        failures.add("$path (${FixtureLoader.load(path).getString("id")}): ${error.message}")
      }
    }
    if (failures.isNotEmpty()) {
      throw AssertionError("Equivalence fixture failures:\n${failures.joinToString("\n")}")
    }
  }
}
