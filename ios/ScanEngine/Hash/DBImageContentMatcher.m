#import "DBImageContentMatcher.h"

#import "DBDHash.h"
#import "DBImageConstants.h"

@implementation DBImageContentMatcher

+ (BOOL)matchesLeft:(uint64_t)left
              right:(uint64_t)right
   hammingThreshold:(NSInteger)hammingThreshold
{
  if (hammingThreshold < 0) {
    hammingThreshold = DBImageHammingThresholdDefault;
  }
  return [DBDHash hammingDistanceBetween:left and:right] <= hammingThreshold;
}

@end
