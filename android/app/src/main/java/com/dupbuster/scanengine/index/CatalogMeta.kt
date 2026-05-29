package com.dupbuster.scanengine.index

/** Bridge-facing catalog metadata (`getCatalogMeta`). */
data class CatalogMeta(
    val schemaVersion: Int,
    val fullRescanRequired: Boolean,
)
