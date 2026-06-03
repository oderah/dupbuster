package com.dupbuster.scanengine.fixtures

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * CI gate AC-security-redact-01: every fixture with `expect.ciGate` must pass native telemetry
 * redaction (crash message, structured payload, exception line).
 */
@RunWith(Parameterized::class)
class SecurityCiGateTest(
    private val fixturePath: String,
) {

  @Test
  fun ciGateFixture_passesRedactionPolicy() {
    SecurityCiGateRunner.runRedact(FixtureLoader.load(fixturePath))
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun ciGateFixtures(): List<Array<String>> =
        FixtureLoader.redactCiGateFixturePaths().map { arrayOf(it) }
  }
}
