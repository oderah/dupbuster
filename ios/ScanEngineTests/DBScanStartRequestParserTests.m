#import <XCTest/XCTest.h>

#import "DBScanStartRequest.h"
#import "DBScanStartRequestParser.h"

@interface DBScanStartRequestParserTests : XCTestCase
@end

@implementation DBScanStartRequestParserTests

- (void)testParse_platformDiscoveryWithEmptyRoots
{
  DBScanStartRequest *request =
      [DBScanStartRequestParser parseDictionary:@{
        @"mode" : DBScanRootModeBridgePlatformDiscovery,
        @"roots" : @[],
      }];

  XCTAssertEqual(request.mode, DBScanRootModePlatformDiscovery);
  XCTAssertEqual(request.roots.count, 0u);
  XCTAssertNil(request.resumeScanRunId);
}

- (void)testParse_userSelectedWithRootGrant
{
  DBScanStartRequest *request =
      [DBScanStartRequestParser parseDictionary:@{
        @"mode" : DBScanRootModeBridgeUserSelected,
        @"roots" : @[ @{@"uriGrant" : @"file:///docs"} ],
      }];

  XCTAssertEqual(request.mode, DBScanRootModeUserSelected);
  XCTAssertEqual(request.roots.count, 1u);
  XCTAssertEqualObjects(request.roots.firstObject.uriGrant, @"file:///docs");
  XCTAssertNil(request.roots.firstObject.scanRootId);
}

- (void)testParse_largeFilesOptIn
{
  DBScanStartRequest *request =
      [DBScanStartRequestParser parseDictionary:@{
        @"mode" : DBScanRootModeBridgePlatformDiscovery,
        @"roots" : @[],
        @"largeFilesOptIn" : @YES,
      }];

  XCTAssertTrue(request.largeFilesOptIn);
}

@end
