package com.dupbuster.scanengine.hash

import android.content.Context
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile
import kotlin.math.min

/**
 * Size bucket → quick sample (> 50 MB) → full SHA-256 `RAW_BYTES` (architecture §4.1).
 * Text `TEXT_NFC_LF` and video `VIDEO_CONTENT_V1` are later milestones (M1-08, M1-13+).
 */
class HashPipeline(
    context: Context,
    private val contentReader: FileContentReader = ContentResolverFileContentReader(context),
    private val sizeBucketIndex: SizeBucketIndex = InMemorySizeBucketIndex(),
) {

  fun hash(
      staged: StagedFile,
      settings: HashSettings = HashSettings(),
  ): HashResult {
    if (staged.isSymlink) {
      return HashResult.SymlinkNode(staged)
    }

    if (staged.sizeBytes == 0L) {
      return HashResult.Success(
          HashedFile(
              staged = staged,
              hashValue = NormalizationProfile.EMPTY,
              normalizationProfile = NormalizationProfile.EMPTY,
          ),
      )
    }

    if (staged.sizeBytes > HashConstants.LARGE_FILE_CAP_BYTES && !settings.largeFilesOptIn) {
      return HashResult.Unscannable(UnscannableReason.LARGE_SKIPPED)
    }

    when (
        sizeBucketIndex.register(
            staged.sizeBytes,
            staged.mediaTypeHint,
            isEmpty = false,
        )
    ) {
      SizeBucketDisposition.UNIQUE_SKIP -> return HashResult.SizeBucketSkipped(staged)
      SizeBucketDisposition.NEEDS_HASH -> Unit
    }

    val deadlineMs = System.currentTimeMillis() + HashConstants.HASH_TIMEOUT_MS
    val quickSampleHash: String? =
        if (staged.sizeBytes > HashConstants.SAMPLE_SIZE_THRESHOLD_BYTES) {
          when (val sample = computeQuickSample(staged, deadlineMs)) {
            is QuickSampleOutcome.Ok -> sample.hash
            QuickSampleOutcome.Timeout -> return HashResult.Unscannable(UnscannableReason.HASH_TIMEOUT)
            QuickSampleOutcome.IoFailure -> return HashResult.Unscannable(UnscannableReason.PERMISSION_DENIED)
          }
        } else {
          null
        }

    val fullHash =
        when (val digest = digestFull(staged, deadlineMs)) {
          is StreamDigestOutcome.Ok -> digest.hexDigest
          StreamDigestOutcome.Timeout -> return HashResult.Unscannable(UnscannableReason.HASH_TIMEOUT)
          StreamDigestOutcome.IoFailure -> return HashResult.Unscannable(UnscannableReason.PERMISSION_DENIED)
        }

    return HashResult.Success(
        HashedFile(
            staged = staged,
            hashValue = fullHash,
            normalizationProfile = NormalizationProfile.RAW_BYTES,
            quickSampleHash = quickSampleHash,
        ),
    )
  }

  private fun computeQuickSample(staged: StagedFile, deadlineMs: Long): QuickSampleOutcome {
    if (System.currentTimeMillis() > deadlineMs) {
      return QuickSampleOutcome.Timeout
    }
    val uri = staged.discovered.contentUri
    val sampleLen =
        min(HashConstants.SAMPLE_CHUNK_BYTES.toLong(), staged.sizeBytes).toInt()
    val first =
        contentReader.readRange(uri, offset = 0, length = sampleLen)
            ?: return QuickSampleOutcome.IoFailure
    val lastOffset = maxOf(0L, staged.sizeBytes - sampleLen)
    val last =
        contentReader.readRange(uri, offset = lastOffset, length = sampleLen)
            ?: return QuickSampleOutcome.IoFailure
    return QuickSampleOutcome.Ok(Sha256Hasher.digestQuickSample(first, last))
  }

  private fun digestFull(staged: StagedFile, deadlineMs: Long): StreamDigestOutcome {
    return when (val opened = contentReader.openRead(staged.discovered.contentUri)) {
      ContentOpenOutcome.IoFailure -> StreamDigestOutcome.IoFailure
      is ContentOpenOutcome.Ok ->
          opened.stream.use { stream ->
            Sha256Hasher.digestStream(stream, deadlineMs = deadlineMs)
          }
    }
  }

}

private sealed class QuickSampleOutcome {
  data class Ok(val hash: String) : QuickSampleOutcome()

  data object Timeout : QuickSampleOutcome()

  data object IoFailure : QuickSampleOutcome()
}
