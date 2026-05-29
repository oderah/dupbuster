#import "DBDurationBucketIndex.h"

#import "DBIndexWriter.h"
#import "DBVideoContentMatcher.h"

@implementation DBSqliteDurationBucketIndex {
  DBIndexWriter *_indexWriter;
}

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter
{
  self = [super init];
  if (self) {
    _indexWriter = indexWriter;
  }
  return self;
}

- (DBDurationBucketDisposition)registerDurationMs:(int64_t)durationMs
{
  if (durationMs <= 0) {
    return DBDurationBucketDispositionUnknown;
  }
  NSInteger existing = [_indexWriter countVideosWithinDurationGate:durationMs];
  return existing > 0 ? DBDurationBucketDispositionHasGateCandidate
                      : DBDurationBucketDispositionUniqueDuration;
}

@end
