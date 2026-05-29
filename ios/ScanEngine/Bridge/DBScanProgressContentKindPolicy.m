#import "DBScanProgressContentKindPolicy.h"

#import "DBScanPhase.h"
#import "DBScanProgressContentKind.h"

@implementation DBScanProgressContentKindPolicy

+ (NSString *)contentKindForHashingPhaseWithVideoFingerprintPass:(BOOL)isVideoFingerprintPass
{
  return isVideoFingerprintPass ? DBScanProgressContentKindVideoContent : DBScanProgressContentKindNone;
}

+ (NSString *_Nullable)bridgeContentKindForPhase:(NSString *)phase
                                    snapshotKind:(NSString *_Nullable)snapshotKind
{
  if (![phase isEqualToString:DBScanPhaseHashing]) {
    return nil;
  }
  if (snapshotKind == nil) {
    return nil;
  }
  NSAssert([self isAllowedBridgeValue:snapshotKind], @"Invalid contentKind: %@", snapshotKind);
  return snapshotKind;
}

+ (BOOL)isAllowedBridgeValue:(NSString *)value
{
  return [value isEqualToString:DBScanProgressContentKindNone] ||
         [value isEqualToString:DBScanProgressContentKindVideoContent];
}

@end
