#import <Foundation/Foundation.h>

@class DBCatalogDatabase;
@class DBScanRunSnapshot;

NS_ASSUME_NONNULL_BEGIN

extern NSString *const DBCheckpointStoreErrorDomain;

typedef NS_ERROR_ENUM(DBCheckpointStoreErrorDomain, DBCheckpointStoreError) {
  DBCheckpointStoreErrorConflict = 1,
  DBCheckpointStoreErrorNotFound = 2,
  DBCheckpointStoreErrorInvalidTransition = 3,
  DBCheckpointStoreErrorMonotonicCheckpoint = 4,
};

/** Persists `scan_run` checkpoints for resume (architecture §6.3 / FR-SI-03). */
@interface DBCheckpointStore : NSObject

- (instancetype)initWithDatabase:(DBCatalogDatabase *)database;

- (NSInteger)beginRunWithRootId:(NSInteger)rootId
                     generation:(NSInteger)generation
                           error:(NSError * _Nullable * _Nullable)error;

- (nullable DBScanRunSnapshot *)runWithId:(NSInteger)scanRunId;

- (nullable DBScanRunSnapshot *)findResumableRun;

- (nullable DBScanRunSnapshot *)findResumableRunForRootId:(NSInteger)rootId;

- (BOOL)hasConflictingActiveRunForRootId:(NSInteger)rootId
                              generation:(NSInteger)generation
                            excludeRunId:(NSInteger)excludeRunId;

- (BOOL)saveCheckpointForRunId:(NSInteger)scanRunId
               lastProcessedId:(int64_t)lastProcessedId
                         error:(NSError * _Nullable * _Nullable)error;

- (BOOL)pauseRunWithId:(NSInteger)scanRunId error:(NSError * _Nullable * _Nullable)error;

- (nullable DBScanRunSnapshot *)resumeRunWithId:(NSInteger)scanRunId
                                          error:(NSError * _Nullable * _Nullable)error;

- (BOOL)markCancellingRunWithId:(NSInteger)scanRunId error:(NSError * _Nullable * _Nullable)error;

- (BOOL)markCancelledRunWithId:(NSInteger)scanRunId
                    endedAtMs:(int64_t)endedAtMs
               teardownReason:(NSString *_Nullable)teardownReason
                        error:(NSError * _Nullable * _Nullable)error;

- (BOOL)markCompleteRunWithId:(NSInteger)scanRunId
                    endedAtMs:(int64_t)endedAtMs
                        error:(NSError * _Nullable * _Nullable)error;

- (BOOL)markErrorRunWithId:(NSInteger)scanRunId
                 endedAtMs:(int64_t)endedAtMs
            teardownReason:(NSString *_Nullable)teardownReason
                     error:(NSError * _Nullable * _Nullable)error;

- (BOOL)abandonForRestartRunWithId:(NSInteger)scanRunId
                         endedAtMs:(int64_t)endedAtMs
                             error:(NSError * _Nullable * _Nullable)error;

@end

NS_ASSUME_NONNULL_END
