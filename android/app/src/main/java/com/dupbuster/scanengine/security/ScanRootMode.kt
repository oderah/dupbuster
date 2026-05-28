package com.dupbuster.scanengine.security

/** Matches `scan_root.mode` and bridge `ScanRootMode`. */
enum class ScanRootMode {
  USER_SELECTED,
  PLATFORM_DISCOVERY,
  ;

  companion object {
    fun fromBridgeValue(value: String): ScanRootMode? =
        when (value) {
          "user_selected" -> USER_SELECTED
          "platform_discovery" -> PLATFORM_DISCOVERY
          else -> null
        }
  }
}
