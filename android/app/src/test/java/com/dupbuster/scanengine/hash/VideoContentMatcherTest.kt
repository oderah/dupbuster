package com.dupbuster.scanengine.hash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoContentMatcherTest {

  @Test
  fun passesDurationGate_allowsWithinTwoPercentUnderOneSecond() {
    assertTrue(VideoContentMatcher.passesDurationGate(60_000, 60_500))
  }

  @Test
  fun passesDurationGate_rejectsOneSecondFloorViolation() {
    assertFalse(VideoContentMatcher.passesDurationGate(59_000, 60_000))
    assertFalse(VideoContentMatcher.passesDurationGate(59_000, 61_000))
  }

  @Test
  fun framePairsMatch_requiresThreeOfFiveWithinThreshold() {
    val left = longArrayOf(0L, 0L, 0L, 0L, 0L)
    val close = longArrayOf(1L, 1L, 1L, 0L, 0L)
    val far = longArrayOf(-1L, -1L, -1L, -1L, -1L)

    assertTrue(VideoContentMatcher.framePairsMatch(left, close))
    assertFalse(VideoContentMatcher.framePairsMatch(left, far))
  }

  @Test
  fun contentMatches_requiresDurationGateAndFramePairs() {
    val left =
        VideoFingerprint(
            frameHashes = longArrayOf(10, 20, 30, 40, 50),
            durationMs = 60_000,
            videoWidth = 1920,
            videoHeight = 1080,
        )
    val right =
        VideoFingerprint(
            frameHashes = longArrayOf(11, 21, 31, 41, 51),
            durationMs = 60_200,
            videoWidth = 1280,
            videoHeight = 720,
        )
    val durationMismatch = right.copy(durationMs = 62_000)

    assertTrue(VideoContentMatcher.contentMatches(left, right))
    assertFalse(VideoContentMatcher.contentMatches(left, durationMismatch))
  }

  @Test
  fun contentMatches_singleFrameClip_matchesCrossResolution() {
    val left =
        VideoFingerprint(
            frameHashes = longArrayOf(42),
            durationMs = 2_500,
            videoWidth = 1920,
            videoHeight = 1080,
        )
    val right =
        VideoFingerprint(
            frameHashes = longArrayOf(43),
            durationMs = 2_500,
            videoWidth = 1280,
            videoHeight = 720,
        )

    assertTrue(VideoContentMatcher.contentMatches(left, right))
  }
}
