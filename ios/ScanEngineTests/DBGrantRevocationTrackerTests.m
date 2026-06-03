#import <XCTest/XCTest.h>

#import "DBGrantRevocationTracker.h"
#import "DBUnscannableReason.h"

@interface DBGrantRevocationTrackerTests : XCTestCase
@end

@implementation DBGrantRevocationTrackerTests

- (void)testIsGrantRevocation_falseBeforeSuccessfulAccess
{
  DBGrantRevocationTracker *tracker = [[DBGrantRevocationTracker alloc] init];
  XCTAssertFalse([tracker isGrantRevocationForScanRootId:1
                                       unscannableReason:DBUnscannableReasonPermissionDenied]);
}

- (void)testIsGrantRevocation_trueAfterSuccessfulAccessOnSameRoot
{
  DBGrantRevocationTracker *tracker = [[DBGrantRevocationTracker alloc] init];
  [tracker markSuccessfulAccessForScanRootId:1];
  XCTAssertTrue([tracker isGrantRevocationForScanRootId:1
                                      unscannableReason:DBUnscannableReasonPermissionDenied]);
}

- (void)testIsGrantRevocation_falseForOtherReasons
{
  DBGrantRevocationTracker *tracker = [[DBGrantRevocationTracker alloc] init];
  [tracker markSuccessfulAccessForScanRootId:1];
  XCTAssertFalse([tracker isGrantRevocationForScanRootId:1
                                       unscannableReason:DBUnscannableReasonHashTimeout]);
}

- (void)testReset_clearsSuccessfulRoots
{
  DBGrantRevocationTracker *tracker = [[DBGrantRevocationTracker alloc] init];
  [tracker markSuccessfulAccessForScanRootId:1];
  [tracker reset];
  XCTAssertFalse([tracker isGrantRevocationForScanRootId:1
                                       unscannableReason:DBUnscannableReasonPermissionDenied]);
}

@end
