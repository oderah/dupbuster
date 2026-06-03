#import <XCTest/XCTest.h>

#import "DBScanBackgroundContinuation.h"
#import "DBScanBackgroundContinuationConstants.h"

@interface DBScanBackgroundContinuationTests : XCTestCase
@end

@implementation DBScanBackgroundContinuationTests

- (void)setUp
{
  [super setUp];
  DBScanBackgroundContinuation *continuation = DBScanBackgroundContinuation.shared;
  [continuation notifyScanEnded];
}

- (void)testTaskIdentifier_isStable
{
  XCTAssertEqualObjects(DBScanBackgroundContinuationTaskIdentifier,
                        @"com.dupbuster.scan.background-continuation.v1");
}

- (void)testScheduleContinuation_withoutActiveScan_returnsNo
{
  DBScanBackgroundContinuation *continuation = DBScanBackgroundContinuation.shared;
  [continuation notifyScanEnded];
  XCTAssertEqual(continuation.trackedActiveScanRunId, 0);
  XCTAssertFalse([continuation scheduleContinuationTaskIfNeeded]);
}

- (void)testNotifyActive_thenEnded_clearsTrackedRunId
{
  DBScanBackgroundContinuation *continuation = DBScanBackgroundContinuation.shared;
  [continuation notifyActiveScanRunId:42];
  XCTAssertEqual(continuation.trackedActiveScanRunId, 42);
  [continuation notifyScanEnded];
  XCTAssertEqual(continuation.trackedActiveScanRunId, 0);
}

- (void)testRegisterApplicationIntegration_isIdempotent
{
  DBScanBackgroundContinuation *continuation = DBScanBackgroundContinuation.shared;
  XCTAssertNoThrow([continuation registerApplicationIntegration]);
  XCTAssertNoThrow([continuation registerApplicationIntegration]);
}

@end
