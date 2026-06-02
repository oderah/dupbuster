package com.dupbuster.scanengine.hash

import android.content.Context
import com.dupbuster.scanengine.discovery.MediaTypeHint
import com.dupbuster.scanengine.security.UnscannableReason
import com.dupbuster.scanengine.stat.StagedFile
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Duration pre-bucket (video) → size bucket → quick sample (> 50 MB) → full SHA-256.
 * Image files run `IMAGE_CONTENT_V1` in parallel; video files run `VIDEO_CONTENT_V1` (M1-13 / M3-12).
 */
class HashPipeline(
    context: Context,
    private val contentReader: FileContentReader = ContentResolverFileContentReader(context),
    private val sizeBucketIndex: SizeBucketIndex = InMemorySizeBucketIndex(),
    private val durationBucketIndex: DurationBucketIndex = InMemoryDurationBucketIndex(),
    private val imageFingerprinter: ImageFingerprinter? = null,
    private val videoFingerprinter: VideoFingerprinter? = null,
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

    if (staged.mediaTypeHint == MediaTypeHint.VIDEO) {
      durationBucketIndex.register(staged.durationMs)
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

    val profile =
        if (staged.mediaTypeHint == MediaTypeHint.TEXT) {
          NormalizationProfile.TEXT_NFC_LF
        } else {
          NormalizationProfile.RAW_BYTES
        }

    val fullHash =
        when (val digest = digestFull(staged, profile, deadlineMs)) {
          is FullDigestOutcome.Ok -> digest.hexDigest
          FullDigestOutcome.Timeout -> return HashResult.Unscannable(UnscannableReason.HASH_TIMEOUT)
          FullDigestOutcome.IoFailure -> return HashResult.Unscannable(UnscannableReason.PERMISSION_DENIED)
          FullDigestOutcome.InvalidUtf8 -> return HashResult.Unscannable(UnscannableReason.PERMISSION_DENIED)
        }

    val rawHashed =
        HashedFile(
            staged = staged,
            hashValue = fullHash,
            normalizationProfile = profile,
            quickSampleHash = quickSampleHash,
        )

    val withImage =
        if (staged.mediaTypeHint == MediaTypeHint.IMAGE && imageFingerprinter != null) {
          when (val imageOutcome = imageFingerprinter.fingerprint(staged, settings)) {
            is ImageFingerprinter.Outcome.Success -> {
              val imageHashed =
                  HashedFile(
                      staged = staged,
                      hashValue = imageOutcome.fingerprint.hashValue,
                      normalizationProfile = NormalizationProfile.IMAGE_CONTENT_V1,
                      frameHashesBlob = imageOutcome.fingerprint.frameHashesBlob,
                  )
              HashResult.ImageSuccess(rawBytes = rawHashed, imageContent = imageHashed)
            }
            is ImageFingerprinter.Outcome.Unscannable ->
                HashResult.ImagePartialSuccess(
                    rawBytes = rawHashed,
                    imageUnscannableReason = imageOutcome.reason,
                )
          }
        } else {
          null
        }

    if (withImage != null) {
      return withImage
    }

    if (staged.mediaTypeHint != MediaTypeHint.VIDEO || videoFingerprinter == null) {
      return HashResult.Success(rawHashed)
    }

    return when (val videoOutcome = videoFingerprinter.fingerprint(staged, settings)) {
      is VideoFingerprinter.Outcome.Success -> {
        val videoHashed =
            HashedFile(
                staged = staged,
                hashValue = videoOutcome.fingerprint.hashValue,
                normalizationProfile = NormalizationProfile.VIDEO_CONTENT_V1,
                quickSampleHash = null,
                frameHashesBlob = videoOutcome.fingerprint.frameHashesBlob,
            )
        HashResult.VideoSuccess(rawBytes = rawHashed, videoContent = videoHashed)
      }
      is VideoFingerprinter.Outcome.Unscannable ->
          HashResult.VideoPartialSuccess(
              rawBytes = rawHashed,
              videoUnscannableReason = videoOutcome.reason,
          )
    }
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

  private fun digestFull(
      staged: StagedFile,
      profile: String,
      deadlineMs: Long,
  ): FullDigestOutcome {
    if (profile != NormalizationProfile.TEXT_NFC_LF) {
      return when (val opened = contentReader.openRead(staged.discovered.contentUri)) {
        ContentOpenOutcome.IoFailure -> FullDigestOutcome.IoFailure
        is ContentOpenOutcome.Ok ->
            when (val digest = opened.stream.use { Sha256Hasher.digestStream(it, deadlineMs = deadlineMs) }) {
              is StreamDigestOutcome.Ok -> FullDigestOutcome.Ok(digest.hexDigest)
              StreamDigestOutcome.Timeout -> FullDigestOutcome.Timeout
              StreamDigestOutcome.IoFailure -> FullDigestOutcome.IoFailure
            }
      }
    }

    val raw = readAllBytes(staged, deadlineMs) ?: return FullDigestOutcome.IoFailure
    if (System.currentTimeMillis() > deadlineMs) {
      return FullDigestOutcome.Timeout
    }
    return when (val normalized = TextNormalizer.normalize(raw)) {
      is TextNormalizer.Outcome.Ok ->
          FullDigestOutcome.Ok(Sha256Hasher.digestBytes(normalized.normalizedUtf8))
      TextNormalizer.Outcome.InvalidUtf8 -> FullDigestOutcome.InvalidUtf8
    }
  }

  private fun readAllBytes(staged: StagedFile, deadlineMs: Long): ByteArray? {
    return when (val opened = contentReader.openRead(staged.discovered.contentUri)) {
      ContentOpenOutcome.IoFailure -> null
      is ContentOpenOutcome.Ok ->
          opened.stream.use { stream ->
            val buffer = ByteArray(HashConstants.HASH_READ_BUFFER_BYTES)
            val out = ByteArrayOutputStream()
            while (true) {
              if (System.currentTimeMillis() > deadlineMs) {
                return null
              }
              val read = stream.read(buffer)
              if (read < 0) {
                break
              }
              if (read > 0) {
                out.write(buffer, 0, read)
              }
            }
            out.toByteArray()
          }
    }
  }

}

private sealed class FullDigestOutcome {
  data class Ok(val hexDigest: String) : FullDigestOutcome()

  data object Timeout : FullDigestOutcome()

  data object IoFailure : FullDigestOutcome()

  data object InvalidUtf8 : FullDigestOutcome()
}

private sealed class QuickSampleOutcome {
  data class Ok(val hash: String) : QuickSampleOutcome()

  data object Timeout : QuickSampleOutcome()

  data object IoFailure : QuickSampleOutcome()
}
