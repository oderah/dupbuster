#import <XCTest/XCTest.h>

#import "DBTextNormalizer.h"

@interface DBTextNormalizerTests : XCTestCase
@end

@implementation DBTextNormalizerTests

- (void)testNormalize_crlfAndLfOnly_produceSameBytes
{
  NSData *crlf = [@"line1\r\nline2\n" dataUsingEncoding:NSUTF8StringEncoding];
  NSData *lfOnly = [@"line1\nline2\n" dataUsingEncoding:NSUTF8StringEncoding];

  NSData *crlfNorm = [DBTextNormalizer normalizedUtf8FromRaw:crlf];
  NSData *lfNorm = [DBTextNormalizer normalizedUtf8FromRaw:lfOnly];

  XCTAssertNotNil(crlfNorm);
  XCTAssertNotNil(lfNorm);
  XCTAssertEqualObjects(crlfNorm, lfNorm);
}

- (void)testNormalize_bomStripped_matchesNoBom
{
  NSMutableData *withBom = [NSMutableData dataWithBytes:"\xEF\xBB\xBF" length:3];
  [withBom appendData:[@"same" dataUsingEncoding:NSUTF8StringEncoding]];
  NSData *withoutBom = [@"same" dataUsingEncoding:NSUTF8StringEncoding];

  XCTAssertEqualObjects(
      [DBTextNormalizer normalizedUtf8FromRaw:withoutBom],
      [DBTextNormalizer normalizedUtf8FromRaw:withBom]);
}

- (void)testNormalize_nfdAndNfc_produceSameBytes
{
  NSData *nfc = [@"caf\u00E9" dataUsingEncoding:NSUTF8StringEncoding];
  NSData *nfd = [@"cafe\u0301" dataUsingEncoding:NSUTF8StringEncoding];

  XCTAssertEqualObjects(
      [DBTextNormalizer normalizedUtf8FromRaw:nfc],
      [DBTextNormalizer normalizedUtf8FromRaw:nfd]);
}

- (void)testNormalize_trailingWhitespacePreserved
{
  NSData *withSpaces = [@"hello  " dataUsingEncoding:NSUTF8StringEncoding];
  NSData *withoutSpaces = [@"hello" dataUsingEncoding:NSUTF8StringEncoding];

  NSData *spaced = [DBTextNormalizer normalizedUtf8FromRaw:withSpaces];
  NSData *plain = [DBTextNormalizer normalizedUtf8FromRaw:withoutSpaces];

  XCTAssertEqualObjects(spaced, withSpaces);
  XCTAssertNotEqualObjects(spaced, plain);
}

- (void)testNormalize_invalidUtf8_returnsNil
{
  uint8_t bytes[] = {0xFF, 0xFE};
  NSData *invalid = [NSData dataWithBytes:bytes length:2];
  XCTAssertNil([DBTextNormalizer normalizedUtf8FromRaw:invalid]);
}

@end
