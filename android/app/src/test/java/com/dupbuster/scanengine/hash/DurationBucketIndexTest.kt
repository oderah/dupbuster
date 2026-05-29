package com.dupbuster.scanengine.hash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationBucketIndexTest {

  @Test
  fun register_unknownDuration_returnsDurationUnknown() {
    val index = InMemoryDurationBucketIndex()

    assertEquals(DurationBucketDisposition.DURATION_UNKNOWN, index.register(0))
    assertEquals(DurationBucketDisposition.DURATION_UNKNOWN, index.register(-1))
    assertEquals(0, index.registeredCount())
  }

  @Test
  fun register_firstDuration_isUnique() {
    val index = InMemoryDurationBucketIndex()

    assertEquals(DurationBucketDisposition.UNIQUE_DURATION, index.register(60_000))
    assertEquals(1, index.registeredCount())
  }

  @Test
  fun register_matchingDurationWithinGate_hasCandidate() {
    val index = InMemoryDurationBucketIndex()

    index.register(60_000)
    assertEquals(DurationBucketDisposition.HAS_GATE_CANDIDATE, index.register(60_200))
  }

  @Test
  fun register_durationOutsideGate_isUnique() {
    val index = InMemoryDurationBucketIndex()

    index.register(60_000)
    assertEquals(DurationBucketDisposition.UNIQUE_DURATION, index.register(62_000))
  }
}
