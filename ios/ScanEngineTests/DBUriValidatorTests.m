#import <XCTest/XCTest.h>

#import "DBUriValidator.h"

@interface DBUriValidatorTests : XCTestCase
@end

@implementation DBUriValidatorTests

- (void)testUserSuppliedLocalIdentifier_isPermissionDenied
{
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];
  DBUriValidator *validator = [[DBUriValidator alloc] init];
  DBUriValidationOutcome outcome =
      [validator validateLocalIdentifier:@"AAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"
                         scanRootGrant:grant
                           provenance:DBUriProvenanceUserSupplied];
  XCTAssertEqual(outcome, DBUriValidationOutcomePermissionDenied);
}

- (void)testLocalIdentifierWithDotDot_isPermissionDenied
{
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];
  DBUriValidator *validator = [[DBUriValidator alloc] init];
  DBUriValidationOutcome outcome =
      [validator validateLocalIdentifier:@"AAAA-BBBB/../etc"
                         scanRootGrant:grant
                           provenance:DBUriProvenanceDiscovery];
  XCTAssertEqual(outcome, DBUriValidationOutcomePermissionDenied);
}

- (void)testValidLocalIdentifierFromDiscovery_isAllowed
{
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];
  DBUriValidator *validator = [[DBUriValidator alloc] init];
  DBUriValidationOutcome outcome =
      [validator validateLocalIdentifier:@"A1B2C3D4-E5F6-7890-ABCD-EF1234567890"
                         scanRootGrant:grant
                           provenance:DBUriProvenanceDiscovery];
  XCTAssertEqual(outcome, DBUriValidationOutcomeAllowed);
}

- (void)testFileURLWithTraversal_isPermissionDenied
{
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"file:///var/mobile/Media"
                                           mode:DBScanRootModeUserSelected];
  DBUriValidator *validator = [[DBUriValidator alloc] init];
  NSURL *url = [NSURL fileURLWithPath:@"/var/mobile/Media/../secret/photo.jpg"];
  DBUriValidationOutcome outcome =
      [validator validateFileURL:url scanRootGrant:grant provenance:DBUriProvenanceDiscovery];
  XCTAssertEqual(outcome, DBUriValidationOutcomePermissionDenied);
}

@end
