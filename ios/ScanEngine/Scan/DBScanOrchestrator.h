#import <Foundation/Foundation.h>

#import "DBHashPipeline.h"
#import "DBDiscoveredEntry.h"
#import "DBScanStartRequest.h"
#import "DBStatStage.h"

@class DBCheckpointStore;
@class DBIndexWriter;
@class DBGrouper;
@class DBScanProgressBridge;
@class DBToctouStatVerifier;
@protocol DBScanDiscoveryRunning;

NS_ASSUME_NONNULL_BEGIN

typedef DBStatStageResult *_Nonnull (^DBScanStatFileBlock)(DBDiscoveredEntry *entry, DBScanRootGrant *grant);
typedef DBHashPipeline *_Nonnull (^DBScanHashPipelineFactoryBlock)(DBIndexWriter *indexWriter);

extern NSString *const DBScanOrchestratorErrorDomain;
extern NSInteger const DBScanOrchestratorGrantRevokedSignal;

typedef NS_ERROR_ENUM(DBScanOrchestratorErrorDomain, DBScanOrchestratorError) {
  DBScanOrchestratorErrorActiveScan = 1,
  DBScanOrchestratorErrorNoActiveSession = 2,
  DBScanOrchestratorErrorScanRunMismatch = 3,
  DBScanOrchestratorErrorResumeUnsupported = 4,
  DBScanOrchestratorErrorBeginRunFailed = 5,
};

/** Wires discovery → stat → hash → index → group with throttled progress (M1-19). */
@interface DBScanOrchestrator : NSObject

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter
                   checkpointStore:(DBCheckpointStore *)checkpointStore
                           grouper:(DBGrouper *)grouper
                   discoveryRunner:(id<DBScanDiscoveryRunning>)discoveryRunner
                          statFile:(DBScanStatFileBlock)statFile
               hashPipelineFactory:(DBScanHashPipelineFactoryBlock)hashPipelineFactory
                    progressBridge:(DBScanProgressBridge *)progressBridge
                         emitError:(void (^)(NSDictionary *payload))emitError
                    toctouVerifier:(DBToctouStatVerifier *)toctouVerifier
                         workQueue:(nullable dispatch_queue_t)workQueue NS_DESIGNATED_INITIALIZER;

- (instancetype)init NS_UNAVAILABLE;

- (NSInteger)startScanWithRequest:(DBScanStartRequest *)request error:(NSError *_Nullable *_Nullable)error;

- (BOOL)pauseScanWithId:(NSInteger)scanRunId error:(NSError *_Nullable *_Nullable)error;
- (BOOL)resumeScanWithId:(NSInteger)scanRunId error:(NSError *_Nullable *_Nullable)error;
- (BOOL)cancelScanWithId:(NSInteger)scanRunId error:(NSError *_Nullable *_Nullable)error;

@end

NS_ASSUME_NONNULL_END
