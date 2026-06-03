#import <XCTest/XCTest.h>

#import "DBImageConstants.h"
#import "DBImageContentMatcher.h"

@interface DBImageContentMatcherTests : XCTestCase
@end

@implementation DBImageContentMatcherTests

- (void)testMatches_withinHammingThreshold
{
  uint64_t left = 0x0123456789ABCDEFULL;
  uint64_t close = left ^ (1ULL << 2);
  XCTAssertTrue([DBImageContentMatcher matchesLeft:left right:close hammingThreshold:DBImageHammingThresholdDefault]);
}

- (void)testMatches_rejectsDistantHashes
{
  uint64_t left = 0x0123456789ABCDEFULL;
  uint64_t far = 0x7EDCBA9876543210ULL;
  XCTAssertFalse([DBImageContentMatcher matchesLeft:left right:far hammingThreshold:DBImageHammingThresholdDefault]);
}

- (void)testMatches_acceptsHammingNine_forRecompressedExports
{
  uint64_t left = 0x0123456789ABCDEFULL;
  uint64_t right = left;
  for (NSInteger bit = 0; bit < 9; bit++) {
    right ^= (1ULL << bit);
  }
  XCTAssertTrue([DBImageContentMatcher matchesLeft:left right:right hammingThreshold:DBImageHammingThresholdDefault]);
}

@end
