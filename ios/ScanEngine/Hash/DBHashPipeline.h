#import <Foundation/Foundation.h>

#import "DBHashSettings.h"
#import "DBHashedFile.h"
#import "DBDurationBucketIndex.h"
#import "DBSizeBucketIndex.h"
#import "DBStagedFile.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBHashPipelineOutcome) {
  DBHashPipelineOutcomeSuccess = 0,
  DBHashPipelineOutcomeVideoSuccess = 4,
  DBHashPipelineOutcomeVideoPartialSuccess = 5,
  DBHashPipelineOutcomeImageSuccess = 6,
  DBHashPipelineOutcomeImagePartialSuccess = 7,
  DBHashPipelineOutcomeSizeBucketSkipped = 1,
  DBHashPipelineOutcomeSymlinkNode = 2,
  DBHashPipelineOutcomeUnscannable = 3,
};

@interface DBHashPipelineResult : NSObject

@property (nonatomic, assign) DBHashPipelineOutcome outcome;
@property (nonatomic, strong, nullable) DBHashedFile *hashed;
@property (nonatomic, strong, nullable) DBHashedFile *rawBytesHashed;
@property (nonatomic, strong, nullable) DBHashedFile *videoContentHashed;
@property (nonatomic, strong, nullable) DBHashedFile *imageContentHashed;
@property (nonatomic, strong, nullable) DBStagedFile *staged;
@property (nonatomic, copy, nullable) NSString *unscannableReason;

@end

@class DBVideoFingerprinter;
@class DBImageFingerprinter;

/**
 * Duration pre-bucket (video) → size bucket → quick sample (> 50 MB) → full SHA-256;
 * images run IMAGE_CONTENT_V1; video runs VIDEO_CONTENT_V1 (M1-13 / M3-12).
 */
@interface DBHashPipeline : NSObject

- (instancetype)initWithFileContentReader:(id<DBFileContentReading>)contentReader
                          sizeBucketIndex:(id<DBSizeBucketIndexing>)sizeBucketIndex;

- (instancetype)initWithFileContentReader:(id<DBFileContentReading>)contentReader
                          sizeBucketIndex:(id<DBSizeBucketIndexing>)sizeBucketIndex
                       videoFingerprinter:(nullable DBVideoFingerprinter *)videoFingerprinter;

- (instancetype)initWithFileContentReader:(id<DBFileContentReading>)contentReader
                          sizeBucketIndex:(id<DBSizeBucketIndexing>)sizeBucketIndex
                     durationBucketIndex:(id<DBDurationBucketIndexing>)durationBucketIndex
                       videoFingerprinter:(nullable DBVideoFingerprinter *)videoFingerprinter
                       imageFingerprinter:(nullable DBImageFingerprinter *)imageFingerprinter;

- (DBHashPipelineResult *)hashStagedFile:(DBStagedFile *)staged
                                settings:(DBHashSettings *)settings;

@end

NS_ASSUME_NONNULL_END
