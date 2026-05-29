#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBSizeBucketDisposition) {
  DBSizeBucketDispositionUniqueSkip = 0,
  DBSizeBucketDispositionNeedsHash = 1,
};

@protocol DBSizeBucketIndexing <NSObject>

- (DBSizeBucketDisposition)registerSizeBytes:(int64_t)sizeBytes
                               mediaTypeHint:(NSString *)mediaTypeHint
                                     isEmpty:(BOOL)isEmpty;

@end

@interface DBInMemorySizeBucketIndex : NSObject <DBSizeBucketIndexing>

- (NSInteger)countForSizeBytes:(int64_t)sizeBytes;

@end

NS_ASSUME_NONNULL_END
