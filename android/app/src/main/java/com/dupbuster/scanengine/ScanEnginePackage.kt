package com.dupbuster.scanengine

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class ScanEnginePackage : BaseReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
      if (name == ScanEngineModule.NAME) {
        ScanEngineModule(reactContext)
      } else {
        null
      }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
      ReactModuleInfoProvider {
        mapOf(
            ScanEngineModule.NAME to
                ReactModuleInfo(
                    ScanEngineModule.NAME,
                    ScanEngineModule.NAME,
                    false,
                    false,
                    false,
                    true,
                ),
        )
      }
}
