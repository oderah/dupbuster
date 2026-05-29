#import <Foundation/Foundation.h>

@class DBScanProgressSnapshot;

NS_ASSUME_NONNULL_BEGIN

typedef void (^DBProgressThrottleEmitBlock)(DBScanProgressSnapshot *snapshot, int64_t emittedAtMs);

/** Coalesces scan progress to ≤4 bridge events/s (250 ms minimum interval). */
@interface DBProgressThrottle : NSObject

@property (nonatomic, assign, readonly) int64_t minIntervalMs;

- (instancetype)initWithEmitBlock:(DBProgressThrottleEmitBlock)emitBlock;
- (instancetype)initWithMinIntervalMs:(int64_t)minIntervalMs
                            emitBlock:(DBProgressThrottleEmitBlock)emitBlock NS_DESIGNATED_INITIALIZER;

- (instancetype)init NS_UNAVAILABLE;

- (void)reportSnapshot:(DBScanProgressSnapshot *)snapshot atMs:(int64_t)atMs;
- (BOOL)advanceToMs:(int64_t)atMs;
- (void)flushAtMs:(int64_t)atMs;
- (void)reset;

@end

FOUNDATION_EXPORT const int64_t DBProgressThrottleMinIntervalMs;
FOUNDATION_EXPORT const NSInteger DBProgressThrottleMaxEventsPerSecond;

NS_ASSUME_NONNULL_END
