#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Hamming match for SAME_CONTENT_IMAGE (FR-FP-09). */
@interface DBImageContentMatcher : NSObject

+ (BOOL)matchesLeft:(uint64_t)left
              right:(uint64_t)right
   hammingThreshold:(NSInteger)hammingThreshold;

@end

NS_ASSUME_NONNULL_END
