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
    val frameHashesBlob: ByteArray? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as HashedFile
    return staged == other.staged &&
        hashValue == other.hashValue &&
        normalizationProfile == other.normalizationProfile &&
        quickSampleHash == other.quickSampleHash &&
        (frameHashesBlob == null && other.frameHashesBlob == null ||
            frameHashesBlob != null &&
                other.frameHashesBlob != null &&
                frameHashesBlob.contentEquals(other.frameHashesBlob))
  }

  override fun hashCode(): Int {
    var result = staged.hashCode()
    result = 31 * result + hashValue.hashCode()
    result = 31 * result + normalizationProfile.hashCode()
    result = 31 * result + (quickSampleHash?.hashCode() ?: 0)
    result = 31 * result + (frameHashesBlob?.contentHashCode() ?: 0)
    return result
  }
}
