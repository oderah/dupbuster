#import "DBScanProgressSnapshot.h"

@implementation DBScanProgressSnapshot

- (instancetype)initWithFilesProcessed:(NSInteger)filesProcessed
                      filesTotalKnown:(NSNumber *)filesTotalKnown
                          groupsFound:(NSInteger)groupsFound
                  reclaimableBytesEst:(int64_t)reclaimableBytesEst
                                phase:(NSString *)phase
                          contentKind:(NSString *)contentKind
{
  self = [super init];
  if (self) {
    _filesProcessed = filesProcessed;
    _filesTotalKnown = filesTotalKnown;
    _groupsFound = groupsFound;
    _reclaimableBytesEst = reclaimableBytesEst;
    _phase = [phase copy];
    _contentKind = [contentKind copy];
  }
  return self;
}

@end
