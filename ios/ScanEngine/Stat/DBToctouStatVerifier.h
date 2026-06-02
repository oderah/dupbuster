#import <Foundation/Foundation.h>

#import "DBFileStat.h"
#import "DBFileStatReading.h"
#import "DBStagedFile.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, DBToctouVerifyOutcome) {
  DBToctouVerifyOutcomeConsistent = 0,
  DBToctouVerifyOutcomeChanged,
  DBToctouVerifyOutcomeIoFailure,
};

/** Re-reads size/mtime after hash and compares to StatStage baseline (FR-SI-01). */
@interface DBToctouStatVerifier : NSObject

@property (nonatomic, strong, readonly) id<DBFileStatReading> fileStatReader;

- (instancetype)initWithFileStatReader:(id<DBFileStatReading>)fileStatReader;

- (DBToctouVerifyOutcome)verifyBaselineForStaged:(DBStagedFile *)staged
                                        freshStat:(DBFileStat *_Nullable *_Nullable)outFreshStat;

- (DBStagedFile *)stagedByApplyingFreshStat:(DBFileStat *)fresh toStaged:(DBStagedFile *)staged;

+ (NSInteger)maxMismatchRetries;

@end

NS_ASSUME_NONNULL_END
