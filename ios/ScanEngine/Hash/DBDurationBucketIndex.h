#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBDurationBucketDisposition) {
  DBDurationBucketDispositionUniqueDuration = 0,
  DBDurationBucketDispositionHasGateCandidate = 1,
  DBDurationBucketDispositionUnknown = 2,
};

@protocol DBDurationBucketIndexing <NSObject>

- (DBDurationBucketDisposition)registerDurationMs:(int64_t)durationMs;

@end

@interface DBInMemoryDurationBucketIndex : NSObject <DBDurationBucketIndexing>

- (NSInteger)registeredCount;

@end

NS_ASSUME_NONNULL_END
