package com.dupbuster.scanengine.fixtures

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** CI gate AC-security-uri-01: crafted SAF `..` docId → PERMISSION_DENIED without open(). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SecurityUriCiGateTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun uriCiGateFixtures_passValidationPolicy() {
    val paths = FixtureLoader.uriCiGateFixturePaths()
    assertTrue("expected at least one uri ciGate fixture", paths.isNotEmpty())
    for (path in paths) {
      SecurityCiGateRunner.runUri(FixtureLoader.load(path), context)
    }
  }
}
