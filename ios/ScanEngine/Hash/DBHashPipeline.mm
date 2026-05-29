#import "DBHashPipeline.h"

#import "DBDiscoveredEntry.h"
#import "DBHashConstants.h"
#import "DBNormalizationProfile.h"
#import "DBSha256Hasher.h"
#import "DBTextNormalizer.h"
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

  NSString *profile =
      [staged.mediaTypeHint isEqualToString:DBMediaTypeHintText]
          ? DBNormalizationProfileTextNfcLf
          : DBNormalizationProfileRawBytes;

  NSString *fullDigest = [self fullDigestForURL:fileURL profile:profile deadline:deadline];
  if (fullDigest == nil) {
    result.outcome = DBHashPipelineOutcomeUnscannable;
    result.unscannableReason =
        ([NSDate.date compare:deadline] == NSOrderedDescending)
            ? DBUnscannableReasonHashTimeout
            : DBUnscannableReasonPermissionDenied;
    return result;
  }

  result.outcome = DBHashPipelineOutcomeSuccess;
  result.hashed = [[DBHashedFile alloc] initWithStaged:staged
                                             hashValue:fullDigest
                                  normalizationProfile:profile
                                       quickSampleHash:quickSampleHash];
  return result;
}

- (nullable NSString *)fullDigestForURL:(NSURL *)fileURL
                               profile:(NSString *)profile
                              deadline:(NSDate *)deadline
{
  if (![profile isEqualToString:DBNormalizationProfileTextNfcLf]) {
    NSInputStream *stream = [self.contentReader openReadForFileURL:fileURL];
    if (stream == nil) {
      return nil;
    }
    NSString *digest = nil;
    DBStreamDigestOutcome digestOutcome =
        [DBSha256Hasher hexDigestOfStream:stream
                               bufferSize:DBHashReadBufferBytes
                                 deadline:deadline
                                outDigest:&digest];
    if (digestOutcome != DBStreamDigestOutcomeOk) {
      return nil;
    }
    return digest;
  }

  NSData *raw = [self readAllBytesForURL:fileURL deadline:deadline];
  if (raw == nil) {
    return nil;
  }
  NSData *normalized = [DBTextNormalizer normalizedUtf8FromRaw:raw];
  if (normalized == nil) {
    return nil;
  }
  return [DBSha256Hasher hexDigestOfData:normalized];
}

- (nullable NSData *)readAllBytesForURL:(NSURL *)fileURL deadline:(NSDate *)deadline
{
  NSInputStream *stream = [self.contentReader openReadForFileURL:fileURL];
  if (stream == nil) {
    return nil;
  }
  [stream open];
  NSMutableData *accumulated = [NSMutableData data];
  uint8_t buffer[DBHashReadBufferBytes];
  while (true) {
    if ([NSDate.date compare:deadline] == NSOrderedDescending) {
      [stream close];
      return nil;
    }
    NSInteger read = [stream read:buffer maxLength:sizeof(buffer)];
    if (read < 0) {
      [stream close];
      return nil;
    }
    if (read == 0) {
      break;
    }
    [accumulated appendBytes:buffer length:(NSUInteger)read];
  }
  [stream close];
  return accumulated;
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
