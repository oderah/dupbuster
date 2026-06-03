package com.dupbuster.scanengine.fixtures

import com.dupbuster.scanengine.security.RedactionFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Descriptor-backed expectations for security-redact-01 (AC-security-redact-01). */
class SecurityRedactFixtureTest {

  @Test
  fun securityRedact01_rawPathMustNotAppearAfterApply() {
    val fixture = FixtureLoader.load("security/security-redact-01.json")
    assertEquals("security-redact-01", fixture.getString("id"))
    assertEquals("both", fixture.getString("platform"))

    val rawPath = fixture.getJSONObject("input").getString("rawPath")
    val expect = fixture.getJSONObject("expect")
    assertTrue(expect.getBoolean("mustNotContainRawPath"))

    val redacted = RedactionFilter.apply(rawPath)!!
    assertFalse(redacted.contains(rawPath))
    assertTrue(redacted.contains(RedactionFilter.REDACTED_PLACEHOLDER))
  }
}
