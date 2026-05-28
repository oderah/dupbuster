package com.dupbuster.scanengine.security

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UriValidatorTest {

  private lateinit var validator: UriValidator

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    validator = UriValidator(context)
  }

  private val treeGrant =
      ScanRootGrant(
          uriGrant =
              Uri.parse(
                  "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
              ),
          mode = ScanRootMode.USER_SELECTED,
      )

  @Test
  fun userSuppliedProvenance_isDenied() {
    val uri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments/a.jpg",
        )
    val result =
        validator.validate(uri, treeGrant, UriProvenance.USER_SUPPLIED)
    assertTrue(result is UriValidationResult.Denied)
    assertEqualsPermissionDenied(result)
  }

  @Test
  fun fileScheme_isDenied() {
    val uri = Uri.parse("file:///sdcard/DCIM/photo.jpg")
    val result = validator.validate(uri, treeGrant, UriProvenance.DISCOVERY)
    assertTrue(result is UriValidationResult.Denied)
  }

  @Test
  fun craftedSafDocIdWithDotDot_isDenied() {
    val uri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2F..%2Fsecret",
        )
    val result = validator.validate(uri, treeGrant, UriProvenance.DISCOVERY)
    assertTrue(result is UriValidationResult.Denied)
    assertEqualsPermissionDenied(result)
  }

  @Test
  fun documentUnderTreeGrant_isAllowed() {
    val uri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fphotos%2Fa.jpg",
        )
    val result = validator.validate(uri, treeGrant, UriProvenance.DISCOVERY)
    assertTrue(result is UriValidationResult.Allowed)
  }

  @Test
  fun documentOutsideTreeGrant_isDenied() {
    val uri =
        Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADownload%2Fother.jpg",
        )
    val result = validator.validate(uri, treeGrant, UriProvenance.DISCOVERY)
    assertTrue(result is UriValidationResult.Denied)
  }

  @Test
  fun mediaStoreUri_allowedInPlatformDiscovery() {
    val grant =
        ScanRootGrant(
            uriGrant = Uri.parse("content://dupbuster/scan-root/platform-discovery"),
            mode = ScanRootMode.PLATFORM_DISCOVERY,
        )
    val uri =
        Uri.parse(
            "content://com.android.providers.media.documents/document/image%3A12345",
        )
    val result = validator.validate(uri, grant, UriProvenance.DISCOVERY)
    assertTrue(result is UriValidationResult.Allowed)
  }

  @Test
  fun unknownAuthority_inUserSelectedMode_isDenied() {
    val uri = Uri.parse("content://evil.provider/document/abc")
    val result = validator.validate(uri, treeGrant, UriProvenance.DISCOVERY)
    assertTrue(result is UriValidationResult.Denied)
  }

  private fun assertEqualsPermissionDenied(result: UriValidationResult) {
    val denied = result as UriValidationResult.Denied
    assertEquals(UnscannableReason.PERMISSION_DENIED, denied.reason)
  }
}
