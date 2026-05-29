#import "DBProgressThrottle.h"

#import "DBScanProgressSnapshot.h"

const int64_t DBProgressThrottleMinIntervalMs = 250;
const NSInteger DBProgressThrottleMaxEventsPerSecond = 4;

@interface DBProgressThrottle ()
@property (nonatomic, copy) DBProgressThrottleEmitBlock emitBlock;
@property (nonatomic, assign) int64_t lastEmitAtMs;
@property (nonatomic, strong, nullable) DBScanProgressSnapshot *pending;
@end

@implementation DBProgressThrottle

- (instancetype)initWithEmitBlock:(DBProgressThrottleEmitBlock)emitBlock
{
  return [self initWithMinIntervalMs:DBProgressThrottleMinIntervalMs emitBlock:emitBlock];
}

- (instancetype)initWithMinIntervalMs:(int64_t)minIntervalMs
                            emitBlock:(DBProgressThrottleEmitBlock)emitBlock
{
  self = [super init];
  if (self) {
    _minIntervalMs = minIntervalMs;
    _emitBlock = [emitBlock copy];
    _lastEmitAtMs = INT64_MIN;
  }
  return self;
}

- (void)reportSnapshot:(DBScanProgressSnapshot *)snapshot atMs:(int64_t)atMs
{
  self.pending = snapshot;
  if (self.lastEmitAtMs == INT64_MIN || atMs - self.lastEmitAtMs >= self.minIntervalMs) {
    [self emitNowAtMs:atMs];
  }
}

- (BOOL)advanceToMs:(int64_t)atMs
{
  if (!self.pending) {
    return NO;
  }
  if (self.lastEmitAtMs != INT64_MIN && atMs - self.lastEmitAtMs < self.minIntervalMs) {
    return NO;
  }
  [self emitNowAtMs:atMs];
  return YES;
}

- (void)flushAtMs:(int64_t)atMs
{
  if (self.pending) {
    [self emitNowAtMs:atMs];
  }
}

- (void)reset
{
  self.lastEmitAtMs = INT64_MIN;
  self.pending = nil;
}

- (void)emitNowAtMs:(int64_t)atMs
{
  DBScanProgressSnapshot *snapshot = self.pending;
  if (!snapshot) {
    return;
  }
  self.emitBlock(snapshot, atMs);
  self.lastEmitAtMs = atMs;
  self.pending = nil;
}

@end
