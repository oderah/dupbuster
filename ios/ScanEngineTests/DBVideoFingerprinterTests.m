#import <XCTest/XCTest.h>

#import "DBDHash.h"
#import "DBVideoFingerprinter.h"

@interface DBVideoFingerprinterTests : XCTestCase
@end

@implementation DBVideoFingerprinterTests

- (void)testSamplePositions_shortClipUsesSingleFrame
{
  NSArray<NSNumber *> *positions = [DBVideoFingerprinter samplePositionsForDurationMs:2000];
  XCTAssertEqual(positions.count, 1);
  XCTAssertEqualWithAccuracy(positions.firstObject.doubleValue, 0.5, 0.001);
}

- (void)testSamplePositions_longClipUsesFiveFrames
{
  NSArray<NSNumber *> *positions = [DBVideoFingerprinter samplePositionsForDurationMs:10000];
  XCTAssertEqual(positions.count, 5);
}

- (void)testDHash_isStableForSameInput
{
  NSMutableData *pixels = [NSMutableData dataWithLength:16 * 16];
  uint8_t *bytes = pixels.mutableBytes;
  for (NSInteger i = 0; i < 16 * 16; i++) {
    bytes[i] = (uint8_t)(i * 3);
  }
  uint64_t first = [DBDHash hashFromGrayPixels:pixels width:16 height:16];
  uint64_t second = [DBDHash hashFromGrayPixels:pixels width:16 height:16];
  XCTAssertEqual(first, second);
}

@end
