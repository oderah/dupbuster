#import "DBScanErrorBridgeMapper.h"

#import "DBRedactionFilter.h"

NSSet<NSString *> *DBScanErrorBridgeAllowedKeys(void)
{
  static NSSet<NSString *> *keys;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    keys = [NSSet setWithArray:@[@"fileEntryId", @"unscannableReason", @"scanRunId"]];
  });
  return keys;
}

@implementation DBScanErrorBridgeMapper

+ (NSDictionary *)bridgePayloadWithFileEntryId:(NSInteger)fileEntryId
                             unscannableReason:(NSString *)unscannableReason
                                     scanRunId:(NSNumber *)scanRunId
{
  NSMutableDictionary *payload = [@{
    @"fileEntryId" : @(fileEntryId),
    @"unscannableReason" : [DBRedactionFilter apply:unscannableReason],
  } mutableCopy];
  if (scanRunId != nil) {
    payload[@"scanRunId"] = scanRunId;
  }
  [self assertBridgeSafePayload:payload];
  return [payload copy];
}

+ (void)assertBridgeSafePayload:(NSDictionary *)payload
{
  for (NSString *key in payload) {
    NSAssert([DBScanErrorBridgeAllowedKeys() containsObject:key], @"Forbidden bridge field: %@", key);
  }
}

@end
