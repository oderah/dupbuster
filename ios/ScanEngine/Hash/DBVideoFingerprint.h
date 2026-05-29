#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBVideoFingerprint : NSObject

@property (nonatomic, copy) NSArray<NSNumber *> *frameHashes;
@property (nonatomic, assign) int64_t durationMs;
@property (nonatomic, assign) NSInteger videoWidth;
@property (nonatomic, assign) NSInteger videoHeight;

- (instancetype)initWithFrameHashes:(NSArray<NSNumber *> *)frameHashes
                         durationMs:(int64_t)durationMs
                         videoWidth:(NSInteger)videoWidth
                        videoHeight:(NSInteger)videoHeight;

- (NSString *)hashValue;
- (NSData *)frameHashesBlob;

@end

NS_ASSUME_NONNULL_END
