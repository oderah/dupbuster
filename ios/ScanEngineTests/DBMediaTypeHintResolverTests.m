#import <XCTest/XCTest.h>

#import "DBMediaTypeHintResolver.h"

@interface DBMediaTypeHintResolverTests : XCTestCase
@end

@implementation DBMediaTypeHintResolverTests

- (void)testHintForUniformTypeIdentifier_video
{
  DBMediaTypeHint hint = [DBMediaTypeHintResolver hintForUniformTypeIdentifier:@"public.movie"];
  XCTAssertEqualObjects(hint, DBMediaTypeHintVideo);
}

- (void)testHintForFileName_pdf
{
  DBMediaTypeHint hint = [DBMediaTypeHintResolver hintForFileName:@"report.PDF"];
  XCTAssertEqualObjects(hint, DBMediaTypeHintDocument);
}

@end
