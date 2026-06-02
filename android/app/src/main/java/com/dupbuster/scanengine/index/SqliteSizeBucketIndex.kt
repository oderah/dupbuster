package com.dupbuster.scanengine.index

import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.hash.SizeBucketDisposition
import com.dupbuster.scanengine.hash.SizeBucketIndex

/**
 * SQLite-backed size-bucket counts (FR-FP-02); replaces [InMemorySizeBucketIndex] for production scans.
 */
class SqliteSizeBucketIndex(private val indexWriter: IndexWriter) : SizeBucketIndex {

  override fun register(
      sizeBytes: Long,
      mediaTypeHint: MediaTypeHint,
      isEmpty: Boolean,
  ): SizeBucketDisposition {
    if (isEmpty || mediaTypeHint == MediaTypeHint.VIDEO || mediaTypeHint == MediaTypeHint.IMAGE) {
      return SizeBucketDisposition.NEEDS_HASH
    }
    val existing = indexWriter.countIndexedFilesWithSize(sizeBytes)
    return if (existing == 0) {
      SizeBucketDisposition.UNIQUE_SKIP
    } else {
      SizeBucketDisposition.NEEDS_HASH
    }
  }
}
