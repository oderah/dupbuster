#import <XCTest/XCTest.h>

#import "DBRedactionFilter.h"
#import "DBScanTelemetryEgress.h"

static NSString *DBFixtureRootPath(void)
{
  NSString *dir = [NSFileManager defaultManager].currentDirectoryPath;
  while (dir.length > 0) {
    NSString *candidate =
        [dir stringByAppendingPathComponent:@"tests/fixtures/dupbuster/v1/manifest.json"];
    if ([[NSFileManager defaultManager] fileExistsAtPath:candidate]) {
      return [dir stringByAppendingPathComponent:@"tests/fixtures/dupbuster/v1"];
    }
    dir = [dir stringByDeletingLastPathComponent];
  }
  XCTFail(@"Could not locate tests/fixtures/dupbuster/v1");
  return @"";
}

static NSDictionary *DBLoadFixture(NSString *relativePath)
{
  NSString *path = [DBFixtureRootPath() stringByAppendingPathComponent:relativePath];
  NSData *data = [NSData dataWithContentsOfFile:path];
  XCTAssertNotNil(data, @"missing fixture %@", relativePath);
  id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
  XCTAssertTrue([json isKindOfClass:[NSDictionary class]]);
  return json;
}

static NSArray<NSString *> *DBCiGateFixturePaths(void)
{
  NSDictionary *manifest = DBLoadFixture(@"manifest.json");
  NSMutableArray<NSString *> *paths = [NSMutableArray array];
  for (NSDictionary *row in manifest[@"fixtures"]) {
    NSString *relativePath = row[@"path"];
    NSDictionary *fixture = DBLoadFixture(relativePath);
    NSDictionary *expect = fixture[@"expect"];
    if ([expect[@"ciGate"] boolValue]) {
      [paths addObject:relativePath];
    }
  }
  [paths sortUsingSelector:@selector(compare:)];
  return paths;
}

static void DBAssertTextMustNotContainRawPath(NSString *text, NSString *rawPath)
{
  XCTAssertFalse([text containsString:rawPath], @"telemetry leaked raw path");
  XCTAssertFalse([DBRedactionFilter containsDeniedContent:text]);
}

static void DBAssertValueMustNotContainRawPath(id value, NSString *rawPath)
{
  if (value == nil || value == [NSNull null]) {
    return;
  }
  if ([value isKindOfClass:[NSString class]]) {
    DBAssertTextMustNotContainRawPath((NSString *)value, rawPath);
    return;
  }
  if ([value isKindOfClass:[NSDictionary class]]) {
    for (id nested in [(NSDictionary *)value allValues]) {
      DBAssertValueMustNotContainRawPath(nested, rawPath);
    }
    return;
  }
  if ([value isKindOfClass:[NSArray class]]) {
    for (id nested in (NSArray *)value) {
      DBAssertValueMustNotContainRawPath(nested, rawPath);
    }
  }
}

static void DBRunSecurityRedact01(NSDictionary *fixture)
{
  NSString *rawPath = fixture[@"input"][@"rawPath"];
  NSDictionary *expect = fixture[@"expect"];
  XCTAssertTrue([expect[@"ciGate"] boolValue]);
  XCTAssertTrue([expect[@"mustNotContainRawPath"] boolValue]);

  NSString *redacted = [DBRedactionFilter apply:rawPath];
  DBAssertTextMustNotContainRawPath(redacted, rawPath);
  XCTAssertTrue([redacted containsString:DBRedactionPlaceholder]);

  NSString *crashMessage =
      [DBScanTelemetryEgress sanitizeCrashMessage:[NSString stringWithFormat:@"open failed at %@", rawPath]];
  DBAssertTextMustNotContainRawPath(crashMessage, rawPath);

  NSDictionary *payload = @{
    @"scan_run_id" : @1,
    @"uri_or_path" : rawPath,
    @"display_name" : @"test.jpg",
    @"detail" : [NSString stringWithFormat:@"failed at %@", rawPath],
  };
  NSDictionary *sanitized = [DBScanTelemetryEgress sanitizeCrashPayload:payload];
  DBAssertValueMustNotContainRawPath(sanitized, rawPath);
  XCTAssertEqualObjects(sanitized[@"uri_or_path"], DBRedactionPlaceholder);
  XCTAssertEqualObjects(sanitized[@"display_name"], DBRedactionPlaceholder);

  NSException *exception = [NSException exceptionWithName:@"TestException"
                                                   reason:[NSString stringWithFormat:@"failed: %@", rawPath]
                                                 userInfo:nil];
  NSString *exceptionLine = [DBScanTelemetryEgress sanitizeExceptionForTelemetry:exception];
  DBAssertTextMustNotContainRawPath(exceptionLine, rawPath);
}

@interface DBSecurityCiGateTests : XCTestCase
@end

@implementation DBSecurityCiGateTests

- (void)testCiGateFixtures_securityRedact01
{
  for (NSString *relativePath in DBCiGateFixturePaths()) {
    NSDictionary *fixture = DBLoadFixture(relativePath);
    NSString *fixtureId = fixture[@"id"];
    if ([fixtureId isEqualToString:@"security-redact-01"]) {
      DBRunSecurityRedact01(fixture);
    } else {
      XCTFail(@"Unhandled ciGate fixture: %@", fixtureId);
    }
  }
  XCTAssertTrue(DBCiGateFixturePaths().count > 0);
}

@end
