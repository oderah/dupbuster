#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBCheckpointStore.h"
#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBGrouper.h"
#import "DBHashPipeline.h"
#import "DBHashSettings.h"
#import "DBHashedFile.h"
#import "DBIndexWriter.h"
#import "DBNormalizationProfile.h"
#import "DBScanOrchestrator.h"
#import "DBScanPhase.h"
#import "DBScanProgressBridge.h"
#import "DBScanRunStatus.h"
#import "DBScanRunSnapshot.h"
#import "DBScanStartRequest.h"
#import "DBSizeBucketIndex.h"
#import "DBStagedFile.h"
#import "DBProductionScanDiscoveryRunner.h"
#import "DBScanRootResolver.h"
#import "DBScanRunStatus.h"
#import "DBToctouStatVerifier.h"
#import "DBUnscannableReason.h"
#import "DBFileStatReading.h"

#import <sqlite3.h>

@interface DBFakeContentReader : NSObject <DBFileContentReading>
@property (nonatomic, copy) NSData *payload;
@end

@implementation DBFakeContentReader

- (nullable NSInputStream *)openReadForFileURL:(NSURL *)fileURL
{
  (void)fileURL;
  return [NSInputStream inputStreamWithData:self.payload];
}

- (nullable NSData *)readRangeForFileURL:(NSURL *)fileURL offset:(int64_t)offset length:(NSUInteger)length
{
  (void)fileURL;
  if (offset >= (int64_t)self.payload.length) {
    return [NSData data];
  }
  NSUInteger end = MIN((NSUInteger)offset + length, self.payload.length);
  return [self.payload subdataWithRange:NSMakeRange((NSUInteger)offset, end - (NSUInteger)offset)];
}

@end

@interface DBFakeDiscoveryRunner : NSObject <DBScanDiscoveryRunning>
@property (nonatomic, copy) NSArray<DBDiscoveredEntry *> *entries;
@end

@implementation DBFakeDiscoveryRunner

- (DBDiscoveryResult *)discoverWithRequest:(DBScanStartRequest *)request
                                      plan:(DBScanRootResolverPlan *)plan
                                generation:(NSInteger)generation
                                   handler:(DBScanDiscoveryEntryHandler)handler
                               isCancelled:(DBScanDiscoveryCancelBlock)isCancelled
{
  (void)request;
  (void)plan;
  (void)isCancelled;
  for (DBDiscoveredEntry *entry in self.entries) {
    handler(entry);
  }
  DBDiscoveryResult *result = [[DBDiscoveryResult alloc] init];
  result.entriesEmitted = self.entries.count;
  return result;
}

@end

@interface DBEchoFileStatReader : NSObject <DBFileStatReading>
@property (nonatomic, strong, nullable) DBStagedFile *latestStaged;
@end

@implementation DBEchoFileStatReader

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error
{
  (void)fileURL;
  (void)error;
  if (self.latestStaged == nil) {
    return nil;
  }
  return [[DBFileStat alloc] initWithSizeBytes:self.latestStaged.sizeBytes
                                       mtimeNs:self.latestStaged.mtimeNs
                                         inode:self.latestStaged.inode
                                      deviceId:self.latestStaged.deviceId
                                     isSymlink:self.latestStaged.isSymlink
                                    durationMs:self.latestStaged.durationMs
                                    videoWidth:self.latestStaged.videoWidth
                                   videoHeight:self.latestStaged.videoHeight];
}

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error
{
  return [self statFileURL:nil error:error];
}

@end

@interface DBToctouSequenceStatReader : NSObject <DBFileStatReading>
@property (nonatomic, assign) NSInteger verifyReads;
@property (nonatomic, strong, nullable) DBStagedFile *latestStaged;
@property (nonatomic, assign) int64_t changedSize;
@property (nonatomic, assign) int64_t changedMtimeNs;
@end

@implementation DBToctouSequenceStatReader

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error
{
  (void)fileURL;
  (void)error;
  self.verifyReads += 1;
  if (self.verifyReads == 1) {
    return [[DBFileStat alloc] initWithSizeBytes:4
                                         mtimeNs:100
                                           inode:nil
                                        deviceId:nil
                                       isSymlink:NO
                                      durationMs:0
                                      videoWidth:0
                                     videoHeight:0];
  }
  if (self.verifyReads == 2) {
    return [[DBFileStat alloc] initWithSizeBytes:self.changedSize
                                         mtimeNs:self.changedMtimeNs
                                           inode:nil
                                        deviceId:nil
                                       isSymlink:NO
                                      durationMs:0
                                      videoWidth:0
                                     videoHeight:0];
  }
  if (self.latestStaged == nil) {
    return nil;
  }
  return [[DBFileStat alloc] initWithSizeBytes:self.latestStaged.sizeBytes
                                       mtimeNs:self.latestStaged.mtimeNs
                                         inode:self.latestStaged.inode
                                      deviceId:self.latestStaged.deviceId
                                     isSymlink:self.latestStaged.isSymlink
                                    durationMs:self.latestStaged.durationMs
                                    videoWidth:self.latestStaged.videoWidth
                                   videoHeight:self.latestStaged.videoHeight];
}

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error
{
  return [self statFileURL:nil error:error];
}

@end

@interface DBIoFailureAfterFirstStatReader : NSObject <DBFileStatReading>
@property (nonatomic, assign) NSInteger verifyReads;
@property (nonatomic, strong, nullable) DBStagedFile *latestStaged;
@end

@implementation DBIoFailureAfterFirstStatReader

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error
{
  (void)fileURL;
  (void)error;
  self.verifyReads += 1;
  if (self.latestStaged == nil) {
    return nil;
  }
  if (self.verifyReads == 1) {
    return [[DBFileStat alloc] initWithSizeBytes:self.latestStaged.sizeBytes
                                         mtimeNs:self.latestStaged.mtimeNs
                                           inode:self.latestStaged.inode
                                        deviceId:self.latestStaged.deviceId
                                       isSymlink:self.latestStaged.isSymlink
                                      durationMs:self.latestStaged.durationMs
                                      videoWidth:self.latestStaged.videoWidth
                                     videoHeight:self.latestStaged.videoHeight];
  }
  return nil;
}

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error
{
  return [self statFileURL:nil error:error];
}

@end

@interface DBScanOrchestratorTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBCheckpointStore *checkpoints;
@property (nonatomic, strong) NSMutableArray<NSString *> *emittedPhases;
@property (nonatomic, strong) DBScanOrchestrator *orchestrator;
@property (nonatomic, strong) DBEchoFileStatReader *echoStatReader;
@end

@interface DBScanOrchestratorTests ()
@property (nonatomic, strong) dispatch_queue_t syncWorkQueue;
@end

@implementation DBScanOrchestratorTests

- (void)setUp
{
  [super setUp];
  self.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([self.database openWithError:&error], @"%@", error);
  self.writer = [[DBIndexWriter alloc] initWithDatabase:self.database];
  self.checkpoints = [[DBCheckpointStore alloc] initWithDatabase:self.database];
  self.emittedPhases = [NSMutableArray array];
  self.syncWorkQueue = dispatch_queue_create("com.dupbuster.test.scan-orchestrator", DISPATCH_QUEUE_SERIAL);

  NSData *payload = [@"duplicate-payload" dataUsingEncoding:NSUTF8StringEncoding];
  DBFakeContentReader *contentReader = [[DBFakeContentReader alloc] init];
  contentReader.payload = payload;

  __weak typeof(self) weakSelf = self;
  DBScanProgressBridge *progressBridge =
      [[DBScanProgressBridge alloc] initWithEmitBlock:^(NSDictionary *payloadMap) {
        [weakSelf.emittedPhases addObject:payloadMap[@"phase"]];
      }];

  DBFakeDiscoveryRunner *discoveryRunner = [[DBFakeDiscoveryRunner alloc] init];
  discoveryRunner.entries = @[
    [self discoveredEntryWithId:1 generation:1],
    [self discoveredEntryWithId:2 generation:1],
  ];

  self.echoStatReader = [[DBEchoFileStatReader alloc] init];
  DBToctouStatVerifier *toctouVerifier =
      [[DBToctouStatVerifier alloc] initWithFileStatReader:self.echoStatReader];

  self.orchestrator =
      [[DBScanOrchestrator alloc] initWithIndexWriter:self.writer
                                      checkpointStore:self.checkpoints
                                              grouper:[[DBGrouper alloc] initWithDatabase:self.database]
                                      discoveryRunner:discoveryRunner
                                             statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                               (void)grant;
                                               DBStatStageResult *result = [[DBStatStageResult alloc] init];
                                               result.outcome = DBStatStageOutcomeSuccess;
                                               DBFileStat *stat = [[DBFileStat alloc] init];
                                               stat.sizeBytes = 15;
                                               stat.mtimeNs = entry.mtimeNs;
                                               stat.inode = @(entry.mtimeNs);
                                               stat.deviceId = @1;
                                               result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
                                               weakSelf.echoStatReader.latestStaged = result.staged;
                                               return result;
                                             }
                                  hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                                    return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                             sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
                                  }
                                           progressBridge:progressBridge
                                                emitError:^(NSDictionary *payloadMap) {
                                                  (void)payloadMap;
                                                }
                                           toctouVerifier:toctouVerifier
                                                workQueue:self.syncWorkQueue];
}

- (void)tearDown
{
  [self.database close];
  [super tearDown];
}

- (void)testStartScan_runsPipeline_andEmitsTerminalComplete
{
  DBScanStartRequest *request = [[DBScanStartRequest alloc] init];
  request.mode = DBScanRootModePlatformDiscovery;
  request.roots = @[];

  NSError *error = nil;
  NSInteger scanRunId = [self.orchestrator startScanWithRequest:request error:&error];
  dispatch_sync(self.syncWorkQueue, ^{});
  XCTAssertNil(error);
  XCTAssertGreaterThan(scanRunId, 0);

  DBScanRunSnapshot *run = [self.checkpoints runWithId:scanRunId];
  XCTAssertNotNil(run);
  XCTAssertEqualObjects(run.status, DBScanRunStatusComplete);
  XCTAssertTrue([self.emittedPhases containsObject:DBScanPhaseDiscovering]);
  XCTAssertTrue([self.emittedPhases containsObject:DBScanPhaseHashing]);
  XCTAssertTrue([self.emittedPhases containsObject:DBScanPhaseGrouping]);
  XCTAssertEqualObjects(self.emittedPhases.lastObject, DBScanPhaseComplete);
  XCTAssertEqual([self.writer fileEntryCount], 2);
  XCTAssertGreaterThanOrEqual([[DBGrouper alloc] initWithDatabase:self.database].rebuildDuplicateGroups.groupsCreated, 1);
}

- (DBDiscoveredEntry *)discoveredEntryWithId:(NSInteger)entryId generation:(NSInteger)generation
{
  NSURL *contentURL = [NSURL URLWithString:[NSString stringWithFormat:@"file:///tmp/file-%ld.bin", (long)entryId]];
  return [[DBDiscoveredEntry alloc] initWithContentURL:contentURL
                                phAssetLocalIdentifier:nil
                                            scanRootId:1
                                            generation:generation
                                           displayName:[NSString stringWithFormat:@"file-%ld.bin", (long)entryId]
                                         mediaTypeHint:DBMediaTypeHintOther
                                             sizeBytes:15
                                               mtimeNs:entryId];
}

- (DBDiscoveredEntry *)discoveredEntryWithUri:(NSString *)uriString
                                   displayName:(NSString *)displayName
                                     scanRootId:(NSInteger)scanRootId
                                     generation:(NSInteger)generation
                                      mtimeNs:(int64_t)mtimeNs
{
  return [[DBDiscoveredEntry alloc] initWithContentURL:[NSURL URLWithString:uriString]
                                phAssetLocalIdentifier:nil
                                            scanRootId:scanRootId
                                            generation:generation
                                           displayName:displayName
                                         mediaTypeHint:DBMediaTypeHintOther
                                             sizeBytes:15
                                               mtimeNs:mtimeNs];
}

- (DBScanOrchestrator *)orchestratorWithDiscoveryRunner:(DBFakeDiscoveryRunner *)discoveryRunner
                                               statFile:(DBScanStatFileBlock)statFile
                                      hashPipelineFactory:(DBScanHashPipelineFactoryBlock)hashPipelineFactory
                                           toctouVerifier:(DBToctouStatVerifier *)toctouVerifier
                                                workQueue:(dispatch_queue_t)workQueue
                                            progressPhases:(NSMutableArray<NSString *> *)progressPhases
                                                emitErrors:(NSMutableArray<NSString *> *)emitErrors
{
  __weak typeof(self) weakSelf = self;
  DBScanProgressBridge *progressBridge =
      [[DBScanProgressBridge alloc] initWithEmitBlock:^(NSDictionary *payloadMap) {
        [progressPhases addObject:payloadMap[@"phase"]];
      }];
  return [[DBScanOrchestrator alloc] initWithIndexWriter:self.writer
                                       checkpointStore:self.checkpoints
                                               grouper:[[DBGrouper alloc] initWithDatabase:self.database]
                                       discoveryRunner:discoveryRunner
                                              statFile:statFile
                                   hashPipelineFactory:hashPipelineFactory
                                        progressBridge:progressBridge
                                             emitError:^(NSDictionary *payloadMap) {
                                               NSString *reason = payloadMap[@"unscannableReason"];
                                               if (reason != nil) {
                                                 [emitErrors addObject:reason];
                                               }
                                               (void)weakSelf;
                                             }
                                        toctouVerifier:toctouVerifier
                                             workQueue:workQueue];
}

- (void)testStartScan_toctouMismatchAfterHash_restatsAndIndexesFreshMetadata
{
  NSData *payload = [@"stable-payload" dataUsingEncoding:NSUTF8StringEncoding];
  __block NSInteger statCalls = 0;
  DBFakeContentReader *contentReader = [[DBFakeContentReader alloc] init];
  contentReader.payload = payload;

  DBFakeDiscoveryRunner *discoveryRunner = [[DBFakeDiscoveryRunner alloc] init];
  discoveryRunner.entries = @[[self discoveredEntryWithId:1 generation:1]];

  DBToctouSequenceStatReader *sequenceReader = [[DBToctouSequenceStatReader alloc] init];
  sequenceReader.changedSize = (int64_t)payload.length;
  sequenceReader.changedMtimeNs = 200;
  DBToctouStatVerifier *toctouVerifier =
      [[DBToctouStatVerifier alloc] initWithFileStatReader:sequenceReader];

  DBScanOrchestrator *toctouOrchestrator =
      [self orchestratorWithDiscoveryRunner:discoveryRunner
                                   statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                     (void)grant;
                                     statCalls += 1;
                                     int64_t size = statCalls == 1 ? 4 : (int64_t)payload.length;
                                     int64_t mtime = statCalls == 1 ? 100 : 200;
                                     DBStatStageResult *result = [[DBStatStageResult alloc] init];
                                     result.outcome = DBStatStageOutcomeSuccess;
                                     DBFileStat *stat = [[DBFileStat alloc] initWithSizeBytes:size
                                                                                      mtimeNs:mtime
                                                                                        inode:nil
                                                                                     deviceId:nil
                                                                                    isSymlink:NO
                                                                                   durationMs:0
                                                                                   videoWidth:0
                                                                                  videoHeight:0];
                                     result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
                                     sequenceReader.latestStaged = result.staged;
                                     return result;
                                   }
                          hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                            (void)writer;
                            return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                     sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
                          }
                               toctouVerifier:toctouVerifier
                                    workQueue:self.syncWorkQueue
                               progressPhases:[NSMutableArray array]
                                   emitErrors:[NSMutableArray array]];

  DBScanStartRequest *request = [[DBScanStartRequest alloc] init];
  request.mode = DBScanRootModePlatformDiscovery;
  request.roots = @[];
  [toctouOrchestrator startScanWithRequest:request error:nil];
  dispatch_sync(self.syncWorkQueue, ^{});

  XCTAssertEqual(statCalls, 2);
  XCTAssertGreaterThanOrEqual(sequenceReader.verifyReads, 3);

  sqlite3_stmt *stmt = NULL;
  sqlite3_prepare_v2(
      self.database.db,
      "SELECT size, mtime_ns FROM file_entry ORDER BY id DESC LIMIT 1",
      -1,
      &stmt,
      NULL);
  XCTAssertEqual(sqlite3_step(stmt), SQLITE_ROW);
  XCTAssertEqual(sqlite3_column_int64(stmt, 0), (int64_t)payload.length);
  XCTAssertEqual(sqlite3_column_int64(stmt, 1), 200);
  sqlite3_finalize(stmt);
}

- (void)testStartScan_fileDeletedMidHash_tombstonesAndCompletesWithoutError
{
  NSData *payload = [@"gone-mid-hash" dataUsingEncoding:NSUTF8StringEncoding];
  NSInteger platformRootId = [self.writer findOrInsertScanRootWithUriOrGrant:DBPlatformDiscoveryMarkerUri
                                                                        mode:DBScanRootModeBridgePlatformDiscovery];
  NSString *entryUri = @"file:///tmp/deleted.bin";
  NSMutableArray<NSString *> *emitErrors = [NSMutableArray array];
  NSMutableArray<NSString *> *phases = [NSMutableArray array];

  DBHashedFile *prior =
      [[DBHashedFile alloc] initWithStaged:[self stagedForUri:entryUri
                                                   scanRootId:platformRootId
                                                    sizeBytes:payload.length
                                                      mtimeNs:100]
                                 hashValue:@"prior-hash"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil
                          frameHashesBlob:nil];
  [self.writer upsertHashedFile:prior generation:1];
  NSInteger priorRunId = [self.writer beginScanRunWithGeneration:1 rootId:platformRootId];
  [self.writer completeScanRunWithId:priorRunId error:nil];

  DBFakeContentReader *contentReader = [[DBFakeContentReader alloc] init];
  contentReader.payload = payload;
  DBFakeDiscoveryRunner *discoveryRunner = [[DBFakeDiscoveryRunner alloc] init];
  discoveryRunner.entries =
      @[[self discoveredEntryWithUri:entryUri
                         displayName:@"deleted.bin"
                          scanRootId:platformRootId
                          generation:2
                             mtimeNs:100]];

  DBIoFailureAfterFirstStatReader *ioFailureReader = [[DBIoFailureAfterFirstStatReader alloc] init];
  DBToctouStatVerifier *toctouVerifier =
      [[DBToctouStatVerifier alloc] initWithFileStatReader:ioFailureReader];

  DBScanOrchestrator *tombstoneOrchestrator =
      [self orchestratorWithDiscoveryRunner:discoveryRunner
                                   statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                     (void)grant;
                                     DBStatStageResult *result = [[DBStatStageResult alloc] init];
                                     result.outcome = DBStatStageOutcomeSuccess;
                                     DBFileStat *stat = [[DBFileStat alloc] initWithSizeBytes:(int64_t)payload.length
                                                                                      mtimeNs:100
                                                                                        inode:@100
                                                                                     deviceId:@1
                                                                                    isSymlink:NO
                                                                                   durationMs:0
                                                                                   videoWidth:0
                                                                                  videoHeight:0];
                                     result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
                                     ioFailureReader.latestStaged = result.staged;
                                     return result;
                                   }
                          hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                            (void)writer;
                            return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                     sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
                          }
                               toctouVerifier:toctouVerifier
                                    workQueue:self.syncWorkQueue
                               progressPhases:phases
                                   emitErrors:emitErrors];

  DBScanStartRequest *request = [[DBScanStartRequest alloc] init];
  request.mode = DBScanRootModePlatformDiscovery;
  request.roots = @[];
  [tombstoneOrchestrator startScanWithRequest:request error:nil];
  dispatch_sync(self.syncWorkQueue, ^{});

  XCTAssertEqualObjects(phases.lastObject, DBScanPhaseComplete);
  XCTAssertEqual(emitErrors.count, 0);
  XCTAssertEqual([self.writer fileEntryCount], 0);
}

- (void)testStartScan_grantRevokedMidScan_pausesAndRetainsPartialResults
{
  NSData *payload = [@"keep-me" dataUsingEncoding:NSUTF8StringEncoding];
  NSInteger platformRootId = [self.writer findOrInsertScanRootWithUriOrGrant:DBPlatformDiscoveryMarkerUri
                                                                        mode:DBScanRootModeBridgePlatformDiscovery];
  NSString *uriOk = @"file:///tmp/ok.bin";
  NSString *uriRevoked = @"file:///tmp/revoked.bin";
  NSMutableArray<NSString *> *phases = [NSMutableArray array];
  NSMutableArray<NSString *> *emitErrors = [NSMutableArray array];
  dispatch_semaphore_t pausedLatch = dispatch_semaphore_create(0);
  dispatch_queue_t asyncQueue = dispatch_queue_create("com.dupbuster.test.grant-revoke", DISPATCH_QUEUE_SERIAL);

  DBFakeContentReader *contentReader = [[DBFakeContentReader alloc] init];
  contentReader.payload = payload;
  DBFakeDiscoveryRunner *discoveryRunner = [[DBFakeDiscoveryRunner alloc] init];
  discoveryRunner.entries = @[
    [self discoveredEntryWithUri:uriOk displayName:@"ok.bin" scanRootId:platformRootId generation:1 mtimeNs:1],
    [self discoveredEntryWithUri:uriRevoked
                     displayName:@"revoked.bin"
                      scanRootId:platformRootId
                      generation:1
                         mtimeNs:2],
  ];

  DBScanProgressBridge *progressBridge =
      [[DBScanProgressBridge alloc] initWithEmitBlock:^(NSDictionary *payloadMap) {
        [phases addObject:payloadMap[@"phase"]];
        if ([payloadMap[@"phase"] isEqualToString:DBScanPhasePaused]) {
          dispatch_semaphore_signal(pausedLatch);
        }
      }];

  DBScanOrchestrator *revokeOrchestrator =
      [[DBScanOrchestrator alloc] initWithIndexWriter:self.writer
                                      checkpointStore:self.checkpoints
                                              grouper:[[DBGrouper alloc] initWithDatabase:self.database]
                                      discoveryRunner:discoveryRunner
                                             statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                               (void)grant;
                                               DBStatStageResult *result = [[DBStatStageResult alloc] init];
                                               if ([entry.contentURL.absoluteString isEqualToString:uriOk]) {
                                                 result.outcome = DBStatStageOutcomeSuccess;
                                                 DBFileStat *stat = [[DBFileStat alloc] initWithSizeBytes:(int64_t)payload.length
                                                                                                  mtimeNs:1
                                                                                                    inode:@1
                                                                                                 deviceId:@1
                                                                                                isSymlink:NO
                                                                                               durationMs:0
                                                                                               videoWidth:0
                                                                                              videoHeight:0];
                                                 result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
                                               } else {
                                                 result.outcome = DBStatStageOutcomeUnscannable;
                                                 result.unscannableReason = DBUnscannableReasonPermissionDenied;
                                               }
                                               return result;
                                             }
                                  hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                                    (void)writer;
                                    return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                             sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
                                  }
                                           progressBridge:progressBridge
                                                emitError:^(NSDictionary *payloadMap) {
                                                  [emitErrors addObject:payloadMap[@"unscannableReason"]];
                                                }
                                           toctouVerifier:[[DBToctouStatVerifier alloc] init]
                                                workQueue:asyncQueue];

  DBScanStartRequest *request = [[DBScanStartRequest alloc] init];
  request.mode = DBScanRootModePlatformDiscovery;
  request.roots = @[];
  NSInteger scanRunId = [revokeOrchestrator startScanWithRequest:request error:nil];
  dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2 * NSEC_PER_SEC));
  XCTAssertEqual(dispatch_semaphore_wait(pausedLatch, timeout), 0);
  XCTAssertTrue([phases containsObject:DBScanPhasePaused]);
  XCTAssertTrue([emitErrors containsObject:DBUnscannableReasonPermissionDenied]);
  DBScanRunSnapshot *run = [self.checkpoints runWithId:scanRunId];
  XCTAssertEqualObjects(run.status, DBScanRunStatusPaused);
  XCTAssertEqual([self.writer fileEntryCount], 1);
  [revokeOrchestrator cancelScanWithId:scanRunId error:nil];
  dispatch_sync(asyncQueue, ^{});
}

- (void)testStartScan_resumeFromCheckpoint_skipsAlreadyIndexedEntries
{
  NSData *payload = [@"resume-payload" dataUsingEncoding:NSUTF8StringEncoding];
  NSInteger platformRootId = [self.writer findOrInsertScanRootWithUriOrGrant:DBPlatformDiscoveryMarkerUri
                                                                        mode:DBScanRootModeBridgePlatformDiscovery];
  NSString *uriFirst = @"file:///tmp/first-resume.bin";
  NSString *uriSecond = @"file:///tmp/second-resume.bin";
  __block NSInteger statInvocations = 0;
  __block NSInteger discoveryPass = 0;

  DBFakeContentReader *contentReader = [[DBFakeContentReader alloc] init];
  contentReader.payload = payload;

  DBFakeDiscoveryRunner *discoveryRunner = [[DBFakeDiscoveryRunner alloc] init];
  discoveryRunner.entries = @[
    [self discoveredEntryWithUri:uriFirst displayName:@"first.bin" scanRootId:platformRootId generation:1 mtimeNs:1],
  ];

  DBScanOrchestrator *firstOrchestrator =
      [self orchestratorWithDiscoveryRunner:discoveryRunner
                                   statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                     (void)grant;
                                     statInvocations += 1;
                                     DBStatStageResult *result = [[DBStatStageResult alloc] init];
                                     result.outcome = DBStatStageOutcomeSuccess;
                                     DBFileStat *stat = [[DBFileStat alloc] initWithSizeBytes:(int64_t)payload.length
                                                                                      mtimeNs:entry.mtimeNs
                                                                                        inode:@(entry.mtimeNs)
                                                                                     deviceId:@1
                                                                                    isSymlink:NO
                                                                                   durationMs:0
                                                                                   videoWidth:0
                                                                                  videoHeight:0];
                                     result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
                                     return result;
                                   }
                          hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                            (void)writer;
                            return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                     sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
                          }
                               toctouVerifier:[[DBToctouStatVerifier alloc] init]
                                    workQueue:self.syncWorkQueue
                               progressPhases:[NSMutableArray array]
                                   emitErrors:[NSMutableArray array]];

  DBScanStartRequest *request = [[DBScanStartRequest alloc] init];
  request.mode = DBScanRootModePlatformDiscovery;
  request.roots = @[];
  NSInteger runId = [firstOrchestrator startScanWithRequest:request error:nil];
  dispatch_sync(self.syncWorkQueue, ^{});
  DBScanRunSnapshot *interrupted = [self.checkpoints runWithId:runId];
  XCTAssertEqualObjects(interrupted.status, DBScanRunStatusComplete);
  XCTAssertEqual(statInvocations, 1);

  statInvocations = 0;
  discoveryPass = 1;
  DBFakeDiscoveryRunner *resumeDiscovery = [[DBFakeDiscoveryRunner alloc] init];
  resumeDiscovery.entries = @[
    [self discoveredEntryWithUri:uriFirst displayName:@"first.bin" scanRootId:platformRootId generation:1 mtimeNs:1],
    [self discoveredEntryWithUri:uriSecond
                     displayName:@"second.bin"
                      scanRootId:platformRootId
                      generation:1
                         mtimeNs:2],
  ];

  NSMutableArray<NSString *> *resumePhases = [NSMutableArray array];
  DBScanOrchestrator *resumeOrchestrator =
      [self orchestratorWithDiscoveryRunner:resumeDiscovery
                                   statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                     (void)grant;
                                     statInvocations += 1;
                                     DBStatStageResult *result = [[DBStatStageResult alloc] init];
                                     result.outcome = DBStatStageOutcomeSuccess;
                                     DBFileStat *stat = [[DBFileStat alloc] initWithSizeBytes:(int64_t)payload.length
                                                                                      mtimeNs:entry.mtimeNs
                                                                                        inode:@(entry.mtimeNs)
                                                                                     deviceId:@1
                                                                                    isSymlink:NO
                                                                                   durationMs:0
                                                                                   videoWidth:0
                                                                                  videoHeight:0];
                                     result.staged = [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
                                     return result;
                                   }
                          hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                            (void)writer;
                            return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                     sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
                          }
                               toctouVerifier:[[DBToctouStatVerifier alloc] init]
                                    workQueue:self.syncWorkQueue
                               progressPhases:resumePhases
                                   emitErrors:[NSMutableArray array]];

  sqlite3_exec(self.database.db,
               "UPDATE scan_run SET status = 'running', ended_at = NULL",
               NULL,
               NULL,
               NULL);

  DBScanStartRequest *resumeRequest = [[DBScanStartRequest alloc] init];
  resumeRequest.mode = DBScanRootModePlatformDiscovery;
  resumeRequest.roots = @[];
  resumeRequest.resumeScanRunId = @(runId);
  [resumeOrchestrator startScanWithRequest:resumeRequest error:nil];
  dispatch_sync(self.syncWorkQueue, ^{});

  XCTAssertEqual(statInvocations, 1);
  XCTAssertTrue([resumePhases containsObject:DBScanPhaseComplete]);
  (void)discoveryPass;
}

- (DBStagedFile *)stagedForUri:(NSString *)uri
                    scanRootId:(NSInteger)scanRootId
                     sizeBytes:(NSUInteger)sizeBytes
                       mtimeNs:(int64_t)mtimeNs
{
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:[NSURL URLWithString:uri]
                             phAssetLocalIdentifier:nil
                                         scanRootId:scanRootId
                                         generation:1
                                        displayName:uri.lastPathComponent
                                      mediaTypeHint:DBMediaTypeHintOther
                                          sizeBytes:(int64_t)sizeBytes
                                            mtimeNs:mtimeNs];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:(int64_t)sizeBytes
                                    mtimeNs:mtimeNs
                                      inode:@(mtimeNs)
                                   deviceId:@1
                                 isSymlink:NO
                               durationMs:0
                               videoWidth:0
                              videoHeight:0];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

@end
