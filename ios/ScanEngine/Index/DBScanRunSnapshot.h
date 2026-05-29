#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBScanRunSnapshot : NSObject

@property (nonatomic, assign) NSInteger scanRunId;
@property (nonatomic, strong, nullable) NSNumber *rootId;
@property (nonatomic, assign) NSInteger generation;
@property (nonatomic, copy) NSString *status;
@property (nonatomic, assign) int64_t lastProcessedId;
@property (nonatomic, assign) int64_t startedAtMs;
@property (nonatomic, strong, nullable) NSNumber *endedAtMs;
@property (nonatomic, copy, nullable) NSString *teardownReason;

@end

NS_ASSUME_NONNULL_END
