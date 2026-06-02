#import "DBToctouStatVerifier.h"

#import "DBDiscoveredEntry.h"
#import "DBFileStatReader.h"

@implementation DBToctouStatVerifier

- (instancetype)initWithFileStatReader:(id<DBFileStatReading>)fileStatReader
{
  self = [super init];
  if (self) {
    _fileStatReader = fileStatReader;
  }
  return self;
}

- (instancetype)init
{
  return [self initWithFileStatReader:[[DBFileStatReader alloc] init]];
}

+ (NSInteger)maxMismatchRetries
{
  return 3;
}

- (DBToctouVerifyOutcome)verifyBaselineForStaged:(DBStagedFile *)staged
                                        freshStat:(DBFileStat *__autoreleasing _Nullable *)outFreshStat
{
  if (outFreshStat != NULL) {
    *outFreshStat = nil;
  }

  DBFileStat *fresh = [self readFreshStatForStaged:staged];
  if (fresh == nil) {
    return DBToctouVerifyOutcomeIoFailure;
  }

  if (fresh.sizeBytes == staged.sizeBytes && fresh.mtimeNs == staged.mtimeNs) {
    return DBToctouVerifyOutcomeConsistent;
  }

  if (outFreshStat != NULL) {
    *outFreshStat = fresh;
  }
  return DBToctouVerifyOutcomeChanged;
}

- (DBStagedFile *)stagedByApplyingFreshStat:(DBFileStat *)fresh toStaged:(DBStagedFile *)staged
{
  DBStagedFile *updated = [[DBStagedFile alloc] initWithDiscovered:staged.discovered fileStat:fresh];
  updated.mediaTypeHint = staged.mediaTypeHint;
  updated.durationMs = staged.durationMs;
  updated.videoWidth = staged.videoWidth;
  updated.videoHeight = staged.videoHeight;
  return updated;
}

- (nullable DBFileStat *)readFreshStatForStaged:(DBStagedFile *)staged
{
  NSError *error = nil;
  if (staged.discovered.phAssetLocalIdentifier.length > 0) {
    return [_fileStatReader statPhAssetLocalIdentifier:staged.discovered.phAssetLocalIdentifier
                                                 error:&error];
  }
  if (staged.discovered.contentURL != nil) {
    return [_fileStatReader statFileURL:staged.discovered.contentURL error:&error];
  }
  return nil;
}

@end
