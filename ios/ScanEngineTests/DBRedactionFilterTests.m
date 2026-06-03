#import <XCTest/XCTest.h>

#import "DBRedactionFilter.h"

@interface DBRedactionFilterTests : XCTestCase
@end

@implementation DBRedactionFilterTests

- (void)testApply_androidStoragePath_mustNotContainRawPath
{
  NSString *raw = @"/storage/emulated/0/DCIM/test.jpg";
  NSString *redacted = [DBRedactionFilter apply:raw];
  XCTAssertFalse([redacted containsString:raw]);
  XCTAssertTrue([redacted containsString:DBRedactionPlaceholder]);
}

- (void)testApply_contentUri_isRedacted
{
  NSString *raw = @"content://media/external/images/media/42";
  NSString *redacted = [DBRedactionFilter apply:raw];
  XCTAssertFalse([redacted containsString:@"content://"]);
}

- (void)testApply_fileUri_isRedacted
{
  NSString *raw = @"file:///var/mobile/Media/DCIM/photo.heic";
  NSString *redacted = [DBRedactionFilter apply:raw];
  XCTAssertFalse([redacted containsString:@"file://"]);
}

- (void)testApply_phAssetUri_isRedacted
{
  NSString *raw = @"ph://ABCDEF-1234-5678";
  NSString *redacted = [DBRedactionFilter apply:raw];
  XCTAssertFalse([redacted containsString:@"ph://"]);
}

- (void)testApply_userHomePath_isRedacted
{
  NSString *raw = @"/Users/test/Pictures/vacation.png";
  NSString *redacted = [DBRedactionFilter apply:raw];
  XCTAssertFalse([redacted containsString:@"/Users/test"]);
}

- (void)testApplyToCrashPayload_sensitiveKeys_forceRedacted
{
  NSDictionary *payload = @{
    @"scan_run_id" : @7,
    @"uri_or_path" : @"/storage/emulated/0/DCIM/test.jpg",
    @"display_name" : @"test.jpg",
    @"nested" : @{@"paths" : @[@"/sdcard/foo.jpg"]},
  };
  NSDictionary *redacted = [DBRedactionFilter applyToCrashPayload:payload];
  XCTAssertEqualObjects(redacted[@"scan_run_id"], @7);
  XCTAssertEqualObjects(redacted[@"uri_or_path"], DBRedactionPlaceholder);
  XCTAssertEqualObjects(redacted[@"display_name"], DBRedactionPlaceholder);
  NSDictionary *nested = redacted[@"nested"];
  XCTAssertEqualObjects(nested[@"paths"], DBRedactionPlaceholder);
}

- (void)testApply_closedSetReason_isUnchanged
{
  XCTAssertEqualObjects([DBRedactionFilter apply:@"PERMISSION_DENIED"], @"PERMISSION_DENIED");
}

@end
