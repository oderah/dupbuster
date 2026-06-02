package com.dupbuster.scanengine.delete

import android.content.Context
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.security.UriValidator
import com.facebook.react.bridge.ReactApplicationContext

object DeleteCoordinatorFactory {

  fun create(
      context: Context,
      platformFileDeleter: PlatformFileDeleter? = null,
  ): DeleteCoordinator {
    val appContext = context.applicationContext
    val deleter =
        platformFileDeleter
            ?: createDefaultPlatformFileDeleter(context, appContext)
    val indexWriter = IndexWriter(CatalogDatabase.getInstance(appContext))
    return DeleteCoordinator(
        indexWriter = indexWriter,
        uriValidator = UriValidator(appContext),
        platformFileDeleter = deleter,
    )
  }

  private fun createDefaultPlatformFileDeleter(
      context: Context,
      appContext: Context,
  ): PlatformFileDeleter {
    if (context is ReactApplicationContext) {
      return AndroidPlatformFileDeleter(
          context = appContext,
          deleteConfirmationLauncher = DeleteIntentLauncher(context),
      )
    }
    return PendingPlatformFileDeleter()
  }
}
