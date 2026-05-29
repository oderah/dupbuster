#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBDHash : NSObject

+ (uint64_t)hashFromGrayPixels:(NSData *)pixels width:(NSInteger)width height:(NSInteger)height;
+ (NSInteger)hammingDistanceBetween:(uint64_t)left and:(uint64_t)right;

@end

NS_ASSUME_NONNULL_END
