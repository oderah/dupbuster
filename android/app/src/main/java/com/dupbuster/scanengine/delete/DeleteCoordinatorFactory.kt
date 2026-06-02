package com.dupbuster.scanengine.delete

import android.content.Context
import com.dupbuster.scanengine.index.CatalogDatabase
import com.dupbuster.scanengine.index.IndexWriter
import com.dupbuster.scanengine.security.UriValidator

object DeleteCoordinatorFactory {

  fun create(
      context: Context,
      platformFileDeleter: PlatformFileDeleter = PendingPlatformFileDeleter(),
  ): DeleteCoordinator {
    val appContext = context.applicationContext
    val indexWriter = IndexWriter(CatalogDatabase.getInstance(appContext))
    return DeleteCoordinator(
        indexWriter = indexWriter,
        uriValidator = UriValidator(appContext),
        platformFileDeleter = platformFileDeleter,
    )
  }
}
