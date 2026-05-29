#import <Foundation/Foundation.h>

@class DBVideoFingerprint;

NS_ASSUME_NONNULL_BEGIN

@interface DBVideoContentMatcher : NSObject

+ (BOOL)passesDurationGateWithDurationA:(int64_t)durationA durationB:(int64_t)durationB;
+ (BOOL)framePairsMatchLeft:(NSArray<NSNumber *> *)left
                      right:(NSArray<NSNumber *> *)right
           hammingThreshold:(NSInteger)hammingThreshold;
+ (BOOL)contentMatchesLeft:(DBVideoFingerprint *)left
                     right:(DBVideoFingerprint *)right
          hammingThreshold:(NSInteger)hammingThreshold;

@end

NS_ASSUME_NONNULL_END
