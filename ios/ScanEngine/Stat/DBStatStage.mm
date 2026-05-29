#import "DBStatStage.h"

#import "DBFileStatReader.h"

@implementation DBStatStageResult
@end

@interface DBStatStage ()
@property (nonatomic, strong) DBUriValidator *uriValidator;
@property (nonatomic, strong) id<DBFileStatReading> fileStatReader;
@end

@implementation DBStatStage

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator
{
  return [self initWithUriValidator:uriValidator fileStatReader:[[DBFileStatReader alloc] init]];
}

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator
                      fileStatReader:(id<DBFileStatReading>)fileStatReader
{
  self = [super init];
  if (self) {
    _uriValidator = uriValidator;
    _fileStatReader = fileStatReader;
  }
  return self;
}

- (DBStatStageResult *)statDiscoveredEntry:(DBDiscoveredEntry *)entry
                            scanRootGrant:(DBScanRootGrant *)grant
                               provenance:(DBUriProvenance)provenance
{
  DBStatStageResult *result = [[DBStatStageResult alloc] init];

  if (entry.phAssetLocalIdentifier.length > 0) {
    if ([self.uriValidator validateLocalIdentifier:entry.phAssetLocalIdentifier
                                     scanRootGrant:grant
                                       provenance:provenance] !=
        DBUriValidationOutcomeAllowed) {
      result.outcome = DBStatStageOutcomeUnscannable;
      result.unscannableReason = DBUnscannableReasonPermissionDenied;
      return result;
    }

    NSError *error = nil;
    DBFileStat *fileStat =
        [self.fileStatReader statPhAssetLocalIdentifier:entry.phAssetLocalIdentifier error:&error];
    if (fileStat == nil) {
      result.outcome = DBStatStageOutcomeUnscannable;
      result.unscannableReason = DBUnscannableReasonPermissionDenied;
      return result;
    }

    result.outcome = DBStatStageOutcomeSuccess;
    result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:fileStat];
    return result;
  }

  if (entry.contentURL == nil) {
    result.outcome = DBStatStageOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonPermissionDenied;
    return result;
  }

  if ([self.uriValidator validateFileURL:entry.contentURL
                           scanRootGrant:grant
                             provenance:provenance] != DBUriValidationOutcomeAllowed) {
    result.outcome = DBStatStageOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonPermissionDenied;
    return result;
  }

  NSError *error = nil;
  DBFileStat *fileStat = [self.fileStatReader statFileURL:entry.contentURL error:&error];
  if (fileStat == nil) {
    result.outcome = DBStatStageOutcomeUnscannable;
    result.unscannableReason = DBUnscannableReasonPermissionDenied;
    return result;
  }

  result.outcome = DBStatStageOutcomeSuccess;
  result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:fileStat];
  return result;
}

@end
