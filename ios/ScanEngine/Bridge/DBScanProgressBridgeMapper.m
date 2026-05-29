#import "DBScanProgressBridgeMapper.h"

#import "DBScanProgressContentKindPolicy.h"
#import "DBScanProgressSnapshot.h"

NSSet<NSString *> *DBScanProgressBridgeAllowedKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[
      @"filesProcessed",
      @"filesTotalKnown",
      @"groupsFound",
      @"reclaimableBytesEst",
      @"phase",
      @"contentKind",
    ]];
  });
  return keys;
}

@implementation DBScanProgressBridgeMapper

+ (NSDictionary *)bridgePayloadFromSnapshot:(DBScanProgressSnapshot *)snapshot
{
  NSMutableDictionary *payload = [@{
    @"filesProcessed" : @(snapshot.filesProcessed),
    @"groupsFound" : @(snapshot.groupsFound),
    @"reclaimableBytesEst" : @(snapshot.reclaimableBytesEst),
    @"phase" : snapshot.phase,
  } mutableCopy];
  if (snapshot.filesTotalKnown != nil) {
    payload[@"filesTotalKnown"] = snapshot.filesTotalKnown;
  } else {
    payload[@"filesTotalKnown"] = [NSNull null];
  }
  NSString *contentKind = [DBScanProgressContentKindPolicy bridgeContentKindForPhase:snapshot.phase
                                                                         snapshotKind:snapshot.contentKind];
  if (contentKind != nil) {
    payload[@"contentKind"] = contentKind;
  }
  [self assertBridgeSafePayload:payload];
  return [payload copy];
}

+ (void)assertBridgeSafePayload:(NSDictionary *)payload
{
  for (NSString *key in payload) {
    NSAssert([DBScanProgressBridgeAllowedKeys() containsObject:key],
             @"Forbidden bridge field: %@",
             key);
  }
  id contentKind = payload[@"contentKind"];
  if (contentKind != nil && contentKind != [NSNull null]) {
    NSAssert([contentKind isKindOfClass:[NSString class]], @"contentKind must be string");
    NSAssert([DBScanProgressContentKindPolicy isAllowedBridgeValue:contentKind],
             @"Invalid contentKind: %@",
             contentKind);
  }
}

@end
