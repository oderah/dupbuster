#import "DBDurationBucketIndex.h"

#import "DBVideoContentMatcher.h"

@implementation DBInMemoryDurationBucketIndex {
  NSMutableArray<NSNumber *> *_durations;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _durations = [NSMutableArray array];
  }
  return self;
}

- (DBDurationBucketDisposition)registerDurationMs:(int64_t)durationMs
{
  if (durationMs <= 0) {
    return DBDurationBucketDispositionUnknown;
  }
  BOOL hasCandidate = NO;
  for (NSNumber *existing in _durations) {
    if ([DBVideoContentMatcher passesDurationGateWithDurationA:durationMs durationB:existing.longLongValue]) {
      hasCandidate = YES;
      break;
    }
  }
  [_durations addObject:@(durationMs)];
  return hasCandidate ? DBDurationBucketDispositionHasGateCandidate : DBDurationBucketDispositionUniqueDuration;
}

- (NSInteger)registeredCount
{
  return _durations.count;
}

@end
