#import "DBVideoContentMatcher.h"

#import "DBDHash.h"
#import "DBVideoConstants.h"
#import "DBVideoFingerprint.h"

@implementation DBVideoContentMatcher

+ (BOOL)passesDurationGateWithDurationA:(int64_t)durationA durationB:(int64_t)durationB
{
  if (durationA <= 0 || durationB <= 0) {
    return NO;
  }
  int64_t minDuration = MIN(durationA, durationB);
  int64_t delta = llabs(durationA - durationB);
  if (delta >= DBVideoDurationGateMinMs) {
    return NO;
  }
  return delta <= (int64_t)(minDuration * DBVideoDurationGateRatio);
}

+ (BOOL)framePairsMatchLeft:(NSArray<NSNumber *> *)left
                      right:(NSArray<NSNumber *> *)right
           hammingThreshold:(NSInteger)hammingThreshold
{
  if (left.count == 0 || right.count == 0) {
    return NO;
  }
  NSUInteger pairCount = MIN(left.count, right.count);
  NSInteger matchingPairs = 0;
  for (NSUInteger index = 0; index < pairCount; index++) {
    uint64_t a = left[index].unsignedLongLongValue;
    uint64_t b = right[index].unsignedLongLongValue;
    if ([DBDHash hammingDistanceBetween:a and:b] <= hammingThreshold) {
      matchingPairs++;
    }
  }
  return matchingPairs >= DBVideoMinMatchingFramePairs;
}

+ (BOOL)contentMatchesLeft:(DBVideoFingerprint *)left
                     right:(DBVideoFingerprint *)right
          hammingThreshold:(NSInteger)hammingThreshold
{
  if (![self passesDurationGateWithDurationA:left.durationMs durationB:right.durationMs]) {
    return NO;
  }
  return [self framePairsMatchLeft:left.frameHashes right:right.frameHashes hammingThreshold:hammingThreshold];
}

@end
