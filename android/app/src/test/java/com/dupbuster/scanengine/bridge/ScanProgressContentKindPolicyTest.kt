package com.dupbuster.scanengine.bridge

import com.dupbuster.scanengine.discovery.MediaTypeHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanProgressContentKindPolicyTest {

  @Test
  fun forHashingPhase_videoFingerprintPass_emitsVideoContent() {
    assertEquals(
        ScanProgressContentKind.VIDEO_CONTENT,
        ScanProgressContentKindPolicy.forHashingPhase(isVideoFingerprintPass = true),
    )
  }

  @Test
  fun forHashingPhase_nonVideoPass_emitsNone() {
    assertEquals(
        ScanProgressContentKind.NONE,
        ScanProgressContentKindPolicy.forHashingPhase(isVideoFingerprintPass = false),
    )
  }

  @Test
  fun forHashingPhase_nonVideoMedia_skipsVideoContentEvenWhenFlagTrue() {
    assertEquals(
        ScanProgressContentKind.VIDEO_CONTENT,
        ScanProgressContentKindPolicy.forHashingPhase(
            mediaTypeHint = MediaTypeHint.VIDEO,
            isVideoFingerprintPass = true,
        ),
    )
    assertEquals(
        ScanProgressContentKind.NONE,
        ScanProgressContentKindPolicy.forHashingPhase(
            mediaTypeHint = MediaTypeHint.IMAGE,
            isVideoFingerprintPass = false,
        ),
    )
  }

  @Test
  fun bridgeContentKind_nonHashingPhase_omitsField() {
    assertNull(
        ScanProgressContentKindPolicy.bridgeContentKind(
            ScanPhase.DISCOVERING,
            ScanProgressContentKind.VIDEO_CONTENT,
        ),
    )
  }

  @Test
  fun bridgeContentKind_hashingPhase_passesThroughAllowedValues() {
    assertEquals(
        ScanProgressContentKind.VIDEO_CONTENT,
        ScanProgressContentKindPolicy.bridgeContentKind(
            ScanPhase.HASHING,
            ScanProgressContentKind.VIDEO_CONTENT,
        ),
    )
    assertEquals(
        ScanProgressContentKind.NONE,
        ScanProgressContentKindPolicy.bridgeContentKind(ScanPhase.HASHING, ScanProgressContentKind.NONE),
    )
  }
}
