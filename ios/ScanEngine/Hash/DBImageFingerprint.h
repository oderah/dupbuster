#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Successful IMAGE_CONTENT_V1 output (FR-FP-09). */
@interface DBImageFingerprint : NSObject

@property (nonatomic, assign) uint64_t dHash;

- (instancetype)initWithDHash:(uint64_t)dHash;

- (NSString *)hashValue;
- (NSData *)frameHashesBlob;

@end

NS_ASSUME_NONNULL_END
