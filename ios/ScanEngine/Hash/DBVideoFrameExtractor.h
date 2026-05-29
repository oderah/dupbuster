#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBVideoFrameExtractOutcome) {
  DBVideoFrameExtractOutcomeOk = 0,
  DBVideoFrameExtractOutcomeDecodeFailed = 1,
  DBVideoFrameExtractOutcomeTimeout = 2,
};

@interface DBGrayFrame : NSObject
@property (nonatomic, assign) NSInteger width;
@property (nonatomic, assign) NSInteger height;
@property (nonatomic, strong) NSData *pixels;
@end

@protocol DBVideoFrameExtracting <NSObject>
- (DBVideoFrameExtractOutcome)extractFramesForURL:(NSURL *)fileURL
                                   samplePositions:(NSArray<NSNumber *> *)samplePositions
                                         deadline:(NSDate *)deadline
                                           frames:(NSArray<DBGrayFrame *> * _Nullable * _Nullable)frames
                                       durationMs:(int64_t * _Nullable)durationMs
                                       videoWidth:(NSInteger * _Nullable)videoWidth
                                      videoHeight:(NSInteger * _Nullable)videoHeight;
@end

NS_ASSUME_NONNULL_END
