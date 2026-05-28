package com.dupbuster.scanengine.security

sealed class UriValidationResult {
  data object Allowed : UriValidationResult()

  /** Fail-closed: invalid URI / grant / authority → PERMISSION_DENIED (FR-UN-03). */
  data class Denied(
      val reason: String = UnscannableReason.PERMISSION_DENIED,
  ) : UriValidationResult()

  val isAllowed: Boolean
    get() = this is Allowed
}
