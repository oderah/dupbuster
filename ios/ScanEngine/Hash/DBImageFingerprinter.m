#import "DBImageFingerprinter.h"

#import "DBDHash.h"
#import "DBHashSettings.h"
#import "DBImageConstants.h"
#import "DBImageFingerprint.h"
#import "DBStagedFile.h"
#import "DBUnscannableReason.h"

@implementation DBImageFingerprinterResult
@end

@implementation DBImageFingerprinter {
  id<DBImageBitmapExtracting> _bitmapExtractor;
}

- (instancetype)initWithBitmapExtractor:(id<DBImageBitmapExtracting>)bitmapExtractor
{
  self = [super init];
  if (self) {
    _bitmapExtractor = bitmapExtractor;
  }
  return self;
}

- (DBImageFingerprinterResult *)fingerprintStaged:(DBStagedFile *)staged
                                         settings:(DBHashSettings *)settings
{
  DBImageFingerprinterResult *result = [[DBImageFingerprinterResult alloc] init];
  int64_t budget = settings.largeFilesOptIn ? DBImageFingerprintBudgetLargeOptInBytes
                                            : DBImageFingerprintBudgetBytes;
  if (staged.sizeBytes > budget) {
    result.outcome = DBImageFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonImageDecodeFailed;
    return result;
  }

  NSURL *fileURL = staged.discovered.contentURL;
  if (fileURL == nil) {
    result.outcome = DBImageFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonImageDecodeFailed;
    return result;
  }

  NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:DBImageFingerprintTimeoutMs / 1000.0];
  DBGrayFrame *frame = nil;
  DBImageBitmapExtractOutcome extractOutcome =
      [_bitmapExtractor decodeImageForURL:fileURL deadline:deadline frame:&frame];
  if (extractOutcome != DBImageBitmapExtractOutcomeOk || frame == nil) {
    result.outcome = DBImageFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonImageDecodeFailed;
    return result;
  }
  if ([NSDate.date compare:deadline] == NSOrderedDescending) {
    result.outcome = DBImageFingerprinterOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonImageDecodeFailed;
    return result;
  }

  uint64_t dHash = [DBDHash hashFromGrayPixels:frame.pixels width:frame.width height:frame.height];
  result.outcome = DBImageFingerprinterOutcomeSuccess;
  result.fingerprint = [[DBImageFingerprint alloc] initWithDHash:dHash];
  return result;
}

@end
