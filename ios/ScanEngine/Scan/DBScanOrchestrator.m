#import "DBScanOrchestrator.h"

#import "DBCheckpointStore.h"
#import "DBDiscoveredEntry.h"
#import "DBGrouper.h"
#import "DBHashSettings.h"
#import "DBScanErrorBridgeMapper.h"
#import "DBScanPhase.h"
#import "DBScanProgressBridge.h"
#import "DBScanProgressSnapshot.h"
#import "DBScanRootResolver.h"
#import "DBScanRunStatus.h"
#import "DBScanSessionControl.h"
#import "DBStagedFile.h"

NSString *const DBScanOrchestratorErrorDomain = @"com.dupbuster.scanengine.scan.orchestrator";

@interface DBScanActiveSession : NSObject
@property (nonatomic, assign) NSInteger scanRunId;
@property (nonatomic, strong) DBScanSessionControl *control;
@end

@implementation DBScanActiveSession
@end

static DBStagedFile *DBStagedFromDiscovered(DBDiscoveredEntry *entry)
{
  DBStagedFile *staged = [[DBStagedFile alloc] init];
  staged.discovered = entry;
  staged.sizeBytes = entry.sizeBytes;
  staged.mtimeNs = entry.mtimeNs;
  staged.inode = nil;
  staged.deviceId = nil;
  staged.isSymlink = NO;
  staged.mediaTypeHint = entry.mediaTypeHint;
  return staged;
}

@implementation DBScanOrchestrator {
  DBIndexWriter *_indexWriter;
  DBCheckpointStore *_checkpointStore;
  DBGrouper *_grouper;
  id<DBScanDiscoveryRunning> _discoveryRunner;
  DBScanStatFileBlock _statFile;
  DBScanHashPipelineFactoryBlock _hashPipelineFactory;
  DBScanProgressBridge *_progressBridge;
  void (^_emitError)(NSDictionary *payload);
  dispatch_queue_t _workQueue;
  DBScanActiveSession *_Nullable _activeSession;
  NSLock *_sessionLock;
}

- (instancetype)initWithIndexWriter:(DBIndexWriter *)indexWriter
                   checkpointStore:(DBCheckpointStore *)checkpointStore
                           grouper:(DBGrouper *)grouper
                   discoveryRunner:(id<DBScanDiscoveryRunning>)discoveryRunner
                          statFile:(DBScanStatFileBlock)statFile
               hashPipelineFactory:(DBScanHashPipelineFactoryBlock)hashPipelineFactory
                    progressBridge:(DBScanProgressBridge *)progressBridge
                         emitError:(void (^)(NSDictionary *payload))emitError
                         workQueue:(dispatch_queue_t)workQueue
{
  self = [super init];
  if (self) {
    _indexWriter = indexWriter;
    _checkpointStore = checkpointStore;
    _grouper = grouper;
    _discoveryRunner = discoveryRunner;
    _statFile = [statFile copy];
    _hashPipelineFactory = [hashPipelineFactory copy];
    _progressBridge = progressBridge;
    _emitError = [emitError copy];
    _workQueue = workQueue ?: dispatch_queue_create("com.dupbuster.scan.orchestrator", DISPATCH_QUEUE_SERIAL);
    _sessionLock = [[NSLock alloc] init];
  }
  return self;
}

- (NSInteger)startScanWithRequest:(DBScanStartRequest *)request error:(NSError **)error
{
  if (request.resumeScanRunId != nil) {
    if (error != nil) {
      *error = [NSError errorWithDomain:DBScanOrchestratorErrorDomain
                                   code:DBScanOrchestratorErrorResumeUnsupported
                               userInfo:@{
                                 NSLocalizedDescriptionKey :
                                     @"resumeScanRunId is not supported until resume wiring lands",
                               }];
    }
    return 0;
  }

  if (![self assertNoConflictingActiveScanWithError:error]) {
    return 0;
  }

  DBScanRootResolverPlan *plan = [DBScanRootResolver resolveRequest:request indexWriter:_indexWriter];
  NSInteger generation = [_indexWriter nextGenerationForRootId:plan.primaryRootId];
  NSInteger scanRunId = [_indexWriter beginScanRunWithGeneration:generation rootId:plan.primaryRootId];
  if (scanRunId <= 0) {
    if (error != nil) {
      *error = [NSError errorWithDomain:DBScanOrchestratorErrorDomain
                                   code:DBScanOrchestratorErrorBeginRunFailed
                               userInfo:@{NSLocalizedDescriptionKey : @"Failed to begin scan run"}];
    }
    return 0;
  }

  DBScanActiveSession *session = [[DBScanActiveSession alloc] init];
  session.scanRunId = scanRunId;
  session.control = [[DBScanSessionControl alloc] init];
  [_sessionLock lock];
  _activeSession = session;
  [_sessionLock unlock];

  dispatch_async(_workQueue, ^{
    [self runScanWithRequest:request plan:plan generation:generation scanRunId:scanRunId control:session.control];
  });

  return scanRunId;
}

- (BOOL)pauseScanWithId:(NSInteger)scanRunId error:(NSError **)error
{
  DBScanActiveSession *session = [self requireActiveSessionForScanRunId:scanRunId error:error];
  if (session == nil) {
    return NO;
  }
  session.control.paused = YES;
  NSError *checkpointError = nil;
  if (![_checkpointStore pauseRunWithId:scanRunId error:&checkpointError]) {
    if (error != nil) {
      *error = checkpointError;
    }
    return NO;
  }
  [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:0
                                                                       filesTotalKnown:nil
                                                                           groupsFound:0
                                                                   reclaimableBytesEst:0
                                                                                 phase:DBScanPhasePaused
                                                                           contentKind:nil]];
  return YES;
}

- (BOOL)resumeScanWithId:(NSInteger)scanRunId error:(NSError **)error
{
  DBScanActiveSession *session = [self requireActiveSessionForScanRunId:scanRunId error:error];
  if (session == nil) {
    return NO;
  }
  session.control.paused = NO;
  NSError *checkpointError = nil;
  if ([_checkpointStore resumeRunWithId:scanRunId error:&checkpointError] == nil) {
    if (error != nil) {
      *error = checkpointError;
    }
    return NO;
  }
  [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:0
                                                                       filesTotalKnown:nil
                                                                           groupsFound:0
                                                                   reclaimableBytesEst:0
                                                                                 phase:DBScanPhaseHashing
                                                                           contentKind:nil]];
  return YES;
}

- (BOOL)cancelScanWithId:(NSInteger)scanRunId error:(NSError **)error
{
  DBScanActiveSession *session = [self requireActiveSessionForScanRunId:scanRunId error:error];
  if (session == nil) {
    return NO;
  }
  session.control.cancelRequested = YES;
  session.control.paused = NO;
  NSError *checkpointError = nil;
  if (![_checkpointStore markCancellingRunWithId:scanRunId error:&checkpointError]) {
    if (error != nil) {
      *error = checkpointError;
    }
    return NO;
  }
  [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:0
                                                                       filesTotalKnown:nil
                                                                           groupsFound:0
                                                                   reclaimableBytesEst:0
                                                                                 phase:DBScanPhaseCancelling
                                                                           contentKind:nil]];
  return YES;
}

#pragma mark - Pipeline

- (void)runScanWithRequest:(DBScanStartRequest *)request
                      plan:(DBScanRootResolverPlan *)plan
                generation:(NSInteger)generation
                 scanRunId:(NSInteger)scanRunId
                   control:(DBScanSessionControl *)control
{
  [_progressBridge reset];
  @try {
    [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:0
                                                                         filesTotalKnown:nil
                                                                             groupsFound:0
                                                                     reclaimableBytesEst:0
                                                                                   phase:DBScanPhaseDiscovering
                                                                             contentKind:nil]];

    NSMutableArray<DBDiscoveredEntry *> *entries = [NSMutableArray array];
    DBDiscoveryResult *discoveryResult =
        [_discoveryRunner discoverWithRequest:request
                                         plan:plan
                                   generation:generation
                                      handler:^(DBDiscoveredEntry *entry) {
                                        [entries addObject:entry];
                                      }
                                  isCancelled:^{
                                    return control.isCancelled;
                                  }];

    if (control.isCancelled || discoveryResult.cancelled) {
      [self finishCancelledWithScanRunId:scanRunId filesProcessed:0 filesTotalKnown:entries.count];
      return;
    }

    DBHashPipeline *hashPipeline = _hashPipelineFactory(_indexWriter);
    NSInteger totalFiles = entries.count;
    NSInteger filesProcessed = 0;
    NSInteger groupsFound = 0;
    int64_t reclaimableBytesEst = 0;

    [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:0
                                                                         filesTotalKnown:@(totalFiles)
                                                                             groupsFound:0
                                                                     reclaimableBytesEst:0
                                                                                   phase:DBScanPhaseHashing
                                                                             contentKind:nil]];

    for (DBDiscoveredEntry *entry in entries) {
      [control awaitIfPaused];
      if (control.isCancelled) {
        [self finishCancelledWithScanRunId:scanRunId filesProcessed:filesProcessed filesTotalKnown:totalFiles];
        return;
      }

      DBScanRootGrant *grant = [self grantForEntry:entry plan:plan];
      DBStatStageResult *statResult = _statFile(entry, grant);
      NSInteger fileEntryId = 0;
      if (statResult.outcome == DBStatStageOutcomeSuccess) {
        fileEntryId = [self processHashResult:hashPipeline
                                       staged:statResult.staged
                                   generation:generation
                                    scanRunId:scanRunId];
      } else {
        fileEntryId = [_indexWriter upsertUnscannableWithReason:statResult.unscannableReason
                                                         staged:DBStagedFromDiscovered(entry)
                                                     generation:generation];
        _emitError([DBScanErrorBridgeMapper bridgePayloadWithFileEntryId:fileEntryId
                                                       unscannableReason:statResult.unscannableReason
                                                               scanRunId:@(scanRunId)]);
      }

      filesProcessed += 1;
      NSError *checkpointError = nil;
      [_indexWriter updateScanRunCheckpoint:scanRunId lastProcessedId:fileEntryId error:&checkpointError];
      [_progressBridge reportHashingProgressWithFilesProcessed:filesProcessed
                                               filesTotalKnown:@(totalFiles)
                                                   groupsFound:groupsFound
                                           reclaimableBytesEst:reclaimableBytesEst
                                        isVideoFingerprintPass:NO
                                                          atMs:[self nowMs]];
    }

    [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:filesProcessed
                                                                         filesTotalKnown:@(totalFiles)
                                                                             groupsFound:groupsFound
                                                                     reclaimableBytesEst:reclaimableBytesEst
                                                                                   phase:DBScanPhaseGrouping
                                                                             contentKind:nil]];

    DBGrouperRebuildResult *groupResult = [_grouper rebuildDuplicateGroups];
    groupsFound = groupResult.groupsCreated;
    reclaimableBytesEst = groupResult.totalReclaimableBytesEst;

    [_indexWriter purgeEntriesNotSeenInGeneration:plan.primaryRootId generation:generation];
    NSError *completeError = nil;
    [_indexWriter completeScanRunWithId:scanRunId error:&completeError];

    [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:filesProcessed
                                                                         filesTotalKnown:@(totalFiles)
                                                                             groupsFound:groupsFound
                                                                     reclaimableBytesEst:reclaimableBytesEst
                                                                                   phase:DBScanPhaseComplete
                                                                             contentKind:nil]];
  } @catch (NSException *exception) {
    int64_t endedAtMs = [self nowMs];
    [_checkpointStore markErrorRunWithId:scanRunId
                               endedAtMs:endedAtMs
                          teardownReason:exception.name
                                   error:nil];
    [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:0
                                                                         filesTotalKnown:nil
                                                                             groupsFound:0
                                                                     reclaimableBytesEst:0
                                                                                   phase:DBScanPhaseError
                                                                             contentKind:nil]];
  } @finally {
    [_sessionLock lock];
    _activeSession = nil;
    [_sessionLock unlock];
  }
}

- (NSInteger)processHashResult:(DBHashPipeline *)hashPipeline
                        staged:(DBStagedFile *)staged
                    generation:(NSInteger)generation
                     scanRunId:(NSInteger)scanRunId
{
  DBHashPipelineResult *hashResult = [hashPipeline hashStagedFile:staged settings:[[DBHashSettings alloc] init]];
  NSInteger fileEntryId = [_indexWriter persistHashPipelineResult:hashResult staged:staged generation:generation];
  if (hashResult.outcome == DBHashPipelineOutcomeUnscannable ||
      hashResult.outcome == DBHashPipelineOutcomeVideoPartialSuccess) {
    NSString *reason = hashResult.unscannableReason ?: @"PERMISSION_DENIED";
    _emitError([DBScanErrorBridgeMapper bridgePayloadWithFileEntryId:fileEntryId
                                                   unscannableReason:reason
                                                           scanRunId:@(scanRunId)]);
  }
  return fileEntryId;
}

- (void)finishCancelledWithScanRunId:(NSInteger)scanRunId
                      filesProcessed:(NSInteger)filesProcessed
                     filesTotalKnown:(NSInteger)filesTotalKnown
{
  int64_t endedAtMs = [self nowMs];
  [_checkpointStore markCancelledRunWithId:scanRunId
                                 endedAtMs:endedAtMs
                            teardownReason:nil
                                     error:nil];
  [self emitPhaseWithSnapshot:[[DBScanProgressSnapshot alloc] initWithFilesProcessed:filesProcessed
                                                                       filesTotalKnown:@(filesTotalKnown)
                                                                           groupsFound:0
                                                                   reclaimableBytesEst:0
                                                                                 phase:DBScanPhaseCancelled
                                                                           contentKind:nil]];
}

- (DBScanRootGrant *)grantForEntry:(DBDiscoveredEntry *)entry plan:(DBScanRootResolverPlan *)plan
{
  for (DBResolvedScanRoot *root in plan.roots) {
    if (root.scanRootId == entry.scanRootId) {
      return [[DBScanRootGrant alloc] initWithUriGrant:root.uriGrant mode:root.mode];
    }
  }
  DBResolvedScanRoot *fallback = plan.roots.firstObject;
  return [[DBScanRootGrant alloc] initWithUriGrant:fallback.uriGrant mode:fallback.mode];
}

- (void)emitPhaseWithSnapshot:(DBScanProgressSnapshot *)snapshot
{
  int64_t nowMs = [self nowMs];
  [_progressBridge reportSnapshot:snapshot atMs:nowMs];
  [_progressBridge flushAtMs:nowMs];
}

- (int64_t)nowMs
{
  return (int64_t)(NSDate.date.timeIntervalSince1970 * 1000.0);
}

#pragma mark - Session helpers

- (BOOL)assertNoConflictingActiveScanWithError:(NSError **)error
{
  [_sessionLock lock];
  DBScanActiveSession *current = _activeSession;
  [_sessionLock unlock];
  if (current == nil) {
    return YES;
  }
  DBScanRunSnapshot *run = [_checkpointStore runWithId:current.scanRunId];
  if (run == nil) {
    return YES;
  }
  if ([DBScanRunStatusActiveValues() containsObject:run.status]) {
    if (error != nil) {
      *error = [NSError errorWithDomain:DBScanOrchestratorErrorDomain
                                   code:DBScanOrchestratorErrorActiveScan
                               userInfo:@{NSLocalizedDescriptionKey : @"A scan is already active"}];
    }
    return NO;
  }
  return YES;
}

- (DBScanActiveSession *)requireActiveSessionForScanRunId:(NSInteger)scanRunId error:(NSError **)error
{
  [_sessionLock lock];
  DBScanActiveSession *session = _activeSession;
  [_sessionLock unlock];
  if (session == nil) {
    if (error != nil) {
      *error = [NSError errorWithDomain:DBScanOrchestratorErrorDomain
                                   code:DBScanOrchestratorErrorNoActiveSession
                               userInfo:@{NSLocalizedDescriptionKey : @"No active scan session"}];
    }
    return nil;
  }
  if (session.scanRunId != scanRunId) {
    if (error != nil) {
      *error = [NSError errorWithDomain:DBScanOrchestratorErrorDomain
                                   code:DBScanOrchestratorErrorScanRunMismatch
                               userInfo:@{
                                 NSLocalizedDescriptionKey :
                                     [NSString stringWithFormat:@"scanRunId %ld is not the active run", (long)scanRunId],
                               }];
    }
    return nil;
  }
  return session;
}

@end
