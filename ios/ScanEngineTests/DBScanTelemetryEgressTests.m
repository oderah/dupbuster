#import <XCTest/XCTest.h>

#import "DBScanTelemetryEgress.h"

@interface DBScanTelemetryEgressTests : XCTestCase
@end

@implementation DBScanTelemetryEgressTests

- (void)tearDown
{
  [DBScanTelemetryEgress setCrashAnalyticsOptIn:NO];
  [super tearDown];
}

- (void)testDefaultOptInIsOff
{
  XCTAssertFalse([DBScanTelemetryEgress isEgressEnabled]);
}

- (void)testRecordPayloadNoOpsWhenOptInDisabled
{
  BOOL recorded =
      [DBScanTelemetryEgress recordCrashPayload:@{
        @"scan_run_id" : @1,
        @"phase" : @"hashing",
      }];
  XCTAssertFalse(recorded);
}

- (void)testRecordPayloadStripsBannedKeysWhenOptInEnabled
{
  [DBScanTelemetryEgress setCrashAnalyticsOptIn:YES];
  BOOL recorded =
      [DBScanTelemetryEgress recordCrashPayload:@{
        @"scan_run_id" : @1,
        @"phase" : @"hashing",
        @"unscannable_reason" : @"PERMISSION_DENIED",
        @"uri_or_path" : @"/var/mobile/Media/secret.jpg",
      }];
  XCTAssertTrue(recorded);
#if DEBUG
  NSDictionary *payload = [DBScanTelemetryEgress lastRecordedPayloadForTests];
  XCTAssertNil(payload[@"unscannable_reason"]);
  XCTAssertNil(payload[@"uri_or_path"]);
#endif
}

- (void)testRecordPayloadReturnsNoWhenOnlyBannedKeys
{
  [DBScanTelemetryEgress setCrashAnalyticsOptIn:YES];
  BOOL recorded =
      [DBScanTelemetryEgress recordCrashPayload:@{@"unscannable_reason" : @"HASH_TIMEOUT"}];
  XCTAssertFalse(recorded);
}

@end
