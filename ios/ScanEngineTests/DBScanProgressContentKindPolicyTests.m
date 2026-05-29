#import <XCTest/XCTest.h>

#import "DBScanPhase.h"
#import "DBScanProgressContentKind.h"
#import "DBScanProgressContentKindPolicy.h"

@interface DBScanProgressContentKindPolicyTests : XCTestCase
@end

@implementation DBScanProgressContentKindPolicyTests

- (void)testHashingVideoFingerprintPass_returnsVideoContent
{
  XCTAssertEqualObjects(
      [DBScanProgressContentKindPolicy contentKindForHashingPhaseWithVideoFingerprintPass:YES],
      DBScanProgressContentKindVideoContent);
}

- (void)testHashingNonVideoPass_returnsNone
{
  XCTAssertEqualObjects(
      [DBScanProgressContentKindPolicy contentKindForHashingPhaseWithVideoFingerprintPass:NO],
      DBScanProgressContentKindNone);
}

- (void)testBridgeContentKind_nonHashingPhase_returnsNil
{
  XCTAssertNil([DBScanProgressContentKindPolicy bridgeContentKindForPhase:DBScanPhaseDiscovering
                                                             snapshotKind:DBScanProgressContentKindVideoContent]);
}

- (void)testBridgeContentKind_hashingPhase_passesThrough
{
  XCTAssertEqualObjects(
      [DBScanProgressContentKindPolicy bridgeContentKindForPhase:DBScanPhaseHashing
                                                    snapshotKind:DBScanProgressContentKindVideoContent],
      DBScanProgressContentKindVideoContent);
}

@end
