#import <Foundation/Foundation.h>

#import "DBDiscoveredEntry.h"
#import "DBFileStatReading.h"
#import "DBStagedFile.h"
#import "DBUriValidator.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBStatStageOutcome) {
  DBStatStageOutcomeSuccess = 0,
  DBStatStageOutcomeUnscannable = 1,
};

@interface DBStatStageResult : NSObject

@property (nonatomic, assign) DBStatStageOutcome outcome;
@property (nonatomic, strong, nullable) DBStagedFile *staged;
@property (nonatomic, copy, nullable) NSString *unscannableReason;

@end

/**
 * Mandatory stat after discovery and UriValidator (architecture §3.2 StatStage).
 * Re-reads size/mtime/inode/device_id for TOCTOU baseline (FR-SI-01).
 */
@interface DBStatStage : NSObject

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator;

- (instancetype)initWithUriValidator:(DBUriValidator *)uriValidator
                    fileStatReader:(id<DBFileStatReading>)fileStatReader;

- (DBStatStageResult *)statDiscoveredEntry:(DBDiscoveredEntry *)entry
                            scanRootGrant:(DBScanRootGrant *)grant
                               provenance:(DBUriProvenance)provenance;

@end

NS_ASSUME_NONNULL_END
