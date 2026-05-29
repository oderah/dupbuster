package com.dupbuster.scanengine.hash

import com.dupbuster.scanengine.stat.StagedFile

/**
 * Hash pipeline output for IndexWriter (architecture §5).
 * `quickSampleHash` is set for files > 50 MB (FR-FP-01 stage 3).
 */
data class HashedFile(
    val staged: StagedFile,
    val hashValue: String,
    val normalizationProfile: String,
    val quickSampleHash: String? = null,
)
