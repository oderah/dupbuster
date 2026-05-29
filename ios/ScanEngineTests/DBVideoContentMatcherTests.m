#import <XCTest/XCTest.h>

#import "DBVideoContentMatcher.h"
#import "DBVideoFingerprint.h"

@interface DBVideoContentMatcherTests : XCTestCase
@end

@implementation DBVideoContentMatcherTests

- (void)testDurationGate_rejectsOneSecondDifference
{
  XCTAssertFalse([DBVideoContentMatcher passesDurationGateWithDurationA:59000 durationB:60000]);
  XCTAssertTrue([DBVideoContentMatcher passesDurationGateWithDurationA:60000 durationB:60500]);
}

- (void)testFramePairsMatch_requiresThreeOfFive
{
  NSArray<NSNumber *> *left = @[@0, @0, @0, @0, @0];
  NSArray<NSNumber *> *close = @[@1, @1, @1, @0, @0];
  NSArray<NSNumber *> *far = @[@(-1), @(-1), @(-1), @(-1), @(-1)];
  XCTAssertTrue([DBVideoContentMatcher framePairsMatchLeft:left right:close hammingThreshold:8]);
  XCTAssertFalse([DBVideoContentMatcher framePairsMatchLeft:left right:far hammingThreshold:8]);
}

- (void)testContentMatches_requiresDurationAndFrames
{
  DBVideoFingerprint *left =
      [[DBVideoFingerprint alloc] initWithFrameHashes:@[@10, @20, @30, @40, @50]
                                           durationMs:60000
                                           videoWidth:1920
                                          videoHeight:1080];
  DBVideoFingerprint *right =
      [[DBVideoFingerprint alloc] initWithFrameHashes:@[@11, @21, @31, @41, @51]
                                           durationMs:60200
                                           videoWidth:1280
                                          videoHeight:720];
  XCTAssertTrue([DBVideoContentMatcher contentMatchesLeft:left right:right hammingThreshold:8]);
}

- (void)testContentMatches_singleFrameClip_matchesCrossResolution
{
  DBVideoFingerprint *left =
      [[DBVideoFingerprint alloc] initWithFrameHashes:@[@42]
                                           durationMs:2500
                                           videoWidth:1920
                                          videoHeight:1080];
  DBVideoFingerprint *right =
      [[DBVideoFingerprint alloc] initWithFrameHashes:@[@43]
                                           durationMs:2500
                                           videoWidth:1280
                                          videoHeight:720];
  XCTAssertTrue([DBVideoContentMatcher contentMatchesLeft:left right:right hammingThreshold:8]);
}

@end
