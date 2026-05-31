#import "DBScanProgressBridge.h"

#import "DBProgressThrottle.h"
#import "DBScanPhase.h"
#import "DBScanProgressBridgeMapper.h"
#import "DBScanProgressContentKindPolicy.h"

@implementation DBScanProgressBridge {
  DBProgressThrottle *_throttle;
}

- (instancetype)initWithEmitBlock:(DBScanProgressBridgeEmitBlock)emitBlock
{
  self = [super init];
  if (self) {
    _throttle = [[DBProgressThrottle alloc] initWithEmitBlock:^(DBScanProgressSnapshot *snapshot, int64_t emittedAtMs) {
      (void)emittedAtMs;
      NSDictionary *payload = [DBScanProgressBridgeMapper bridgePayloadFromSnapshot:snapshot];
      emitBlock(payload);
    }];
  }
  return self;
}

- (void)reportSnapshot:(DBScanProgressSnapshot *)snapshot atMs:(int64_t)atMs
{
  [_throttle reportSnapshot:snapshot atMs:atMs];
}

- (void)advanceToMs:(int64_t)atMs
{
  [_throttle advanceToMs:atMs];
}

- (void)flushAtMs:(int64_t)atMs
{
  [_throttle flushAtMs:atMs];
}

- (void)reset
{
  [_throttle reset];
}

- (void)reportHashingProgressWithFilesProcessed:(NSInteger)filesProcessed
                               filesTotalKnown:(NSNumber *)filesTotalKnown
                                   groupsFound:(NSInteger)groupsFound
                           reclaimableBytesEst:(int64_t)reclaimableBytesEst
                        isVideoFingerprintPass:(BOOL)isVideoFingerprintPass
                                          atMs:(int64_t)atMs
{
  NSString *contentKind =
      [DBScanProgressContentKindPolicy contentKindForHashingPhaseWithVideoFingerprintPass:isVideoFingerprintPass];
  DBScanProgressSnapshot *snapshot =
      [[DBScanProgressSnapshot alloc] initWithFilesProcessed:filesProcessed
                                             filesTotalKnown:filesTotalKnown
                                                 groupsFound:groupsFound
                                         reclaimableBytesEst:reclaimableBytesEst
                                                       phase:DBScanPhaseHashing
                                                 contentKind:contentKind];
  [self reportSnapshot:snapshot atMs:atMs];
  [self advanceToMs:atMs];
}

@end
