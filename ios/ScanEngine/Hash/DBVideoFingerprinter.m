#import "DBVideoFingerprinter.h"

#import "DBDHash.h"
#import "DBHashSettings.h"
#import "DBStagedFile.h"
#import "DBUnscannableReason.h"
#import "DBVideoConstants.h"
#import "DBVideoFingerprint.h"
#import "DBVideoFrameExtractor.h"

typedef NS_ENUM(NSInteger, DBVideoFingerprinterOutcome) {
  DBVideoFingerprinterOutcomeSuccess = 0,
  DBVideoFingerprinterOutcomeUnscannable = 1,
};

@implementation DBVideoFingerprinterResult
@end

@implementation DBVideoFingerprinter {
  id<DBVideoFrameExtracting> _frameExtractor;
}

- (instancetype)initWithFrameExtractor:(id<DBVideoFrameExtracting>)frameExtractor
{
  self = [super init];
  if (self) {
    _frameExtractor = frameExtractor;
  }
  return self;
}

- (DBVideoFingerprinterResult *)fingerprintStaged:(DBStagedFile *)staged settings:(DBHashSettings *)settings
{
  DBVideoFingerprinterResult *result = [[DBVideoFingerprinterResult alloc] init];
  int64_t budget = settings.largeFilesOptIn ? DBVideoFingerprintBudgetLargeOptInBytes
                                            : DBVideoFingerprintBudgetBytes;
  if (staged.sizeBytes > budget) {
    result.outcome = DBVideoFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonVideoDecodeFailed;
    return result;
  }

  NSURL *fileURL = staged.discovered.contentURL;
  if (fileURL == nil) {
    result.outcome = DBVideoFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonVideoDecodeFailed;
    return result;
  }

  NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:DBVideoFingerprintTimeoutMs / 1000.0];
  NSArray<NSNumber *> *samplePositions = [DBVideoFingerprinter samplePositionsForDurationMs:staged.durationMs];
  NSArray<DBGrayFrame *> *frames = nil;
  int64_t durationMs = 0;
  NSInteger videoWidth = 0;
  NSInteger videoHeight = 0;
  DBVideoFrameExtractOutcome extractOutcome =
      [_frameExtractor extractFramesForURL:fileURL
                           samplePositions:samplePositions
                                  deadline:deadline
                                    frames:&frames
                                durationMs:&durationMs
                                videoWidth:&videoWidth
                               videoHeight:&videoHeight];
  if (extractOutcome != DBVideoFrameExtractOutcomeOk || frames.count == 0) {
    result.outcome = DBVideoFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonVideoDecodeFailed;
    return result;
  }

  NSMutableArray<NSNumber *> *hashes = [NSMutableArray arrayWithCapacity:frames.count];
  for (DBGrayFrame *frame in frames) {
    uint64_t hash = [DBDHash hashFromGrayPixels:frame.pixels width:frame.width height:frame.height];
    [hashes addObject:@(hash)];
  }
  result.outcome = DBVideoFingerprinterOutcomeSuccess;
  result.fingerprint = [[DBVideoFingerprint alloc] initWithFrameHashes:hashes
                                                            durationMs:durationMs > 0 ? durationMs : staged.durationMs
                                                            videoWidth:videoWidth > 0 ? videoWidth : staged.videoWidth
                                                           videoHeight:videoHeight > 0 ? videoHeight : staged.videoHeight];
  return result;
}

+ (NSArray<NSNumber *> *)samplePositionsForDurationMs:(int64_t)durationMs
{
  if (durationMs > 0 && durationMs < DBVideoMinDurationMultiFrameMs) {
    return DBVideoSingleFrameSamplePositions();
  }
  return DBVideoFrameSamplePositions();
}

@end

@implementation DBGrayFrame
@end
