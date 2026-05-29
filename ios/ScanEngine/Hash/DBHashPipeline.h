#import <Foundation/Foundation.h>

#import "DBHashSettings.h"
#import "DBHashedFile.h"
#import "DBSizeBucketIndex.h"
#import "DBStagedFile.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBHashPipelineOutcome) {
  DBHashPipelineOutcomeSuccess = 0,
  DBHashPipelineOutcomeSizeBucketSkipped = 1,
  DBHashPipelineOutcomeSymlinkNode = 2,
  DBHashPipelineOutcomeUnscannable = 3,
};

@interface DBHashPipelineResult : NSObject

@property (nonatomic, assign) DBHashPipelineOutcome outcome;
@property (nonatomic, strong, nullable) DBHashedFile *hashed;
@property (nonatomic, strong, nullable) DBStagedFile *staged;
@property (nonatomic, copy, nullable) NSString *unscannableReason;

@end

/**
 * Size bucket → quick sample (> 50 MB) → full SHA-256 `RAW_BYTES` (architecture §4.1).
 */
@interface DBHashPipeline : NSObject

- (instancetype)initWithFileContentReader:(id<DBFileContentReading>)contentReader
                          sizeBucketIndex:(id<DBSizeBucketIndexing>)sizeBucketIndex;

- (DBHashPipelineResult *)hashStagedFile:(DBStagedFile *)staged
                                settings:(DBHashSettings *)settings;

@end

NS_ASSUME_NONNULL_END
