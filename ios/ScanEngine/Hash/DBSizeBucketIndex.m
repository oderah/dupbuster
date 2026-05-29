#import "DBSizeBucketIndex.h"

#import "DBDiscoveredEntry.h"

@implementation DBInMemorySizeBucketIndex {
  NSMutableDictionary<NSNumber *, NSNumber *> *_counts;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _counts = [NSMutableDictionary dictionary];
  }
  return self;
}

- (DBSizeBucketDisposition)registerSizeBytes:(int64_t)sizeBytes
                               mediaTypeHint:(NSString *)mediaTypeHint
                                     isEmpty:(BOOL)isEmpty
{
  if (isEmpty || [mediaTypeHint isEqualToString:DBMediaTypeHintVideo]) {
    return DBSizeBucketDispositionNeedsHash;
  }
  NSNumber *key = @(sizeBytes);
  NSInteger next = _counts[key].integerValue + 1;
  _counts[key] = @(next);
  return next == 1 ? DBSizeBucketDispositionUniqueSkip : DBSizeBucketDispositionNeedsHash;
}

- (NSInteger)countForSizeBytes:(int64_t)sizeBytes
{
  return _counts[@(sizeBytes)].integerValue;
}

@end
