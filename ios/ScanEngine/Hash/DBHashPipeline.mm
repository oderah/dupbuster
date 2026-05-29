#import "DBHashPipeline.h"

#import "DBHashConstants.h"
#import "DBNormalizationProfile.h"
#import "DBSha256Hasher.h"
#import "DBUnscannableReason.h"

@implementation DBHashPipelineResult
@end

@interface DBHashPipeline ()
@property (nonatomic, strong) id<DBFileContentReading> contentReader;
@property (nonatomic, strong) id<DBSizeBucketIndexing> sizeBucketIndex;
@end

@implementation DBHashPipeline

- (instancetype)initWithFileContentReader:(id<DBFileContentReading>)contentReader
                          sizeBucketIndex:(id<DBSizeBucketIndexing>)sizeBucketIndex
{
  self = [super init];
  if (self) {
    _contentReader = contentReader;
    _sizeBucketIndex = sizeBucketIndex;
  }
  return self;
}

- (DBHashPipelineResult *)hashStagedFile:(DBStagedFile *)staged settings:(DBHashSettings *)settings
{
  DBHashPipelineResult *result = [[DBHashPipelineResult alloc] init];
  result.staged = staged;

  if (staged.isSymlink) {
    result.outcome = DBHashPipelineOutcomeSymlinkNode;
    return result;
  }

  if (staged.sizeBytes == 0) {
    result.outcome = DBHashPipelineOutcomeSuccess;
    result.hashed =
        [[DBHashedFile alloc] initWithStaged:staged
                                   hashValue:DBNormalizationProfileEmpty
                        normalizationProfile:DBNormalizationProfileEmpty
                             quickSampleHash:nil];
    return result;
  }

  if (staged.sizeBytes > DBHashLargeFileCapBytes && !settings.largeFilesOptIn) {
    result.outcome = DBHashPipelineOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonLargeSkipped;
    return result;
  }

  DBSizeBucketDisposition disposition =
      [self.sizeBucketIndex registerSizeBytes:staged.sizeBytes
                                mediaTypeHint:staged.mediaTypeHint
                                      isEmpty:NO];
  if (disposition == DBSizeBucketDispositionUniqueSkip) {
    result.outcome = DBHashPipelineOutcomeSizeBucketSkipped;
    return result;
  }

  NSURL *fileURL = staged.discovered.contentURL;
  if (fileURL == nil) {
    result.outcome = DBHashPipelineOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonPermissionDenied;
    return result;
  }

  NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:DBHashTimeoutSeconds];
  NSString *quickSampleHash = nil;
  if (staged.sizeBytes > DBHashSampleSizeThresholdBytes) {
    NSString *sample = [self computeQuickSampleForURL:fileURL staged:staged deadline:deadline];
    if (sample == nil) {
      result.outcome = DBHashPipelineOutcomeUnscannable;
      result.unscannableReason =
          ([NSDate.date compare:deadline] == NSOrderedDescending)
              ? DBUnscannableReasonHashTimeout
              : DBUnscannableReasonPermissionDenied;
      return result;
    }
    quickSampleHash = sample;
  }

  NSInputStream *stream = [self.contentReader openReadForFileURL:fileURL];
  if (stream == nil) {
    result.outcome = DBHashPipelineOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonPermissionDenied;
    return result;
  }

  NSString *fullDigest = nil;
  DBStreamDigestOutcome digestOutcome =
      [DBSha256Hasher hexDigestOfStream:stream
                             bufferSize:DBHashReadBufferBytes
                               deadline:deadline
                              outDigest:&fullDigest];
  if (digestOutcome == DBStreamDigestOutcomeTimeout) {
    result.outcome = DBHashPipelineOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonHashTimeout;
    return result;
  }
  if (digestOutcome != DBStreamDigestOutcomeOk || fullDigest == nil) {
    result.outcome = DBHashPipelineOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonPermissionDenied;
    return result;
  }

  result.outcome = DBHashPipelineOutcomeSuccess;
  result.hashed = [[DBHashedFile alloc] initWithStaged:staged
                                             hashValue:fullDigest
                                  normalizationProfile:DBNormalizationProfileRawBytes
                                       quickSampleHash:quickSampleHash];
  return result;
}

- (nullable NSString *)computeQuickSampleForURL:(NSURL *)fileURL
                                       staged:(DBStagedFile *)staged
                                     deadline:(NSDate *)deadline
{
  if ([NSDate.date compare:deadline] == NSOrderedDescending) {
    return nil;
  }
  NSUInteger sampleLen = (NSUInteger)MIN(DBHashSampleChunkBytes, (NSUInteger)staged.sizeBytes);
  NSData *first = [self.contentReader readRangeForFileURL:fileURL offset:0 length:sampleLen];
  if (first == nil) {
    return nil;
  }
  int64_t lastOffset = MAX((int64_t)0, staged.sizeBytes - (int64_t)sampleLen);
  NSData *last = [self.contentReader readRangeForFileURL:fileURL offset:lastOffset length:sampleLen];
  if (last == nil) {
    return nil;
  }
  return [DBSha256Hasher hexDigestOfQuickSampleFirst:first last:last];
}

@end
