package com.dupbuster.scanengine.discovery

data class DiscoveryResult(
    val entriesEmitted: Int,
    val entriesDenied: Int,
    val directoriesVisited: Int,
    val cancelled: Boolean,
)
