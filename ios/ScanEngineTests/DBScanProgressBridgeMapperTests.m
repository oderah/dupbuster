#import <XCTest/XCTest.h>

#import "DBScanPhase.h"
#import "DBScanProgressBridgeMapper.h"
#import "DBScanProgressContentKind.h"
#import "DBScanProgressSnapshot.h"

@interface DBScanProgressBridgeMapperTests : XCTestCase
@end

@implementation DBScanProgressBridgeMapperTests

- (void)testBridgePayload_hashingVideoContent_includesContentKind
{
  DBScanProgressSnapshot *snapshot =
      [[DBScanProgressSnapshot alloc] initWithFilesProcessed:42
                                            filesTotalKnown:@100
                                                groupsFound:0
                                        reclaimableBytesEst:0
                                                      phase:DBScanPhaseHashing
                                                contentKind:DBScanProgressContentKindVideoContent];

  NSDictionary *payload = [DBScanProgressBridgeMapper bridgePayloadFromSnapshot:snapshot];

  XCTAssertEqualObjects(payload[@"phase"], DBScanPhaseHashing);
  XCTAssertEqualObjects(payload[@"contentKind"], DBScanProgressContentKindVideoContent);
  XCTAssertEqual(payload.count, 6u);
  XCTAssertFalse([payload objectForKey:@"uri_or_path"]);
}

- (void)testBridgePayload_discovering_omitsContentKind
{
  DBScanProgressSnapshot *snapshot =
      [[DBScanProgressSnapshot alloc] initWithFilesProcessed:1
                                            filesTotalKnown:nil
                                                groupsFound:0
                                        reclaimableBytesEst:0
                                                      phase:DBScanPhaseDiscovering
                                                contentKind:DBScanProgressContentKindVideoContent];

  NSDictionary *payload = [DBScanProgressBridgeMapper bridgePayloadFromSnapshot:snapshot];

  XCTAssertNil(payload[@"contentKind"]);
  XCTAssertEqual(payload.count, 5u);
}

@end
