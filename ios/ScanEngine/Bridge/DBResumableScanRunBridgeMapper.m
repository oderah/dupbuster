#import "DBResumableScanRunBridgeMapper.h"

#import "DBScanRunSnapshot.h"

@implementation DBResumableScanRunBridgeMapper

+ (NSDictionary *)bridgePayloadForSnapshot:(DBScanRunSnapshot *)snapshot
{
  return @{
    @"scanRunId" : @(snapshot.scanRunId),
    @"lastProcessedId" : @(snapshot.lastProcessedId),
    @"status" : snapshot.status ?: @"running",
  };
}

@end
