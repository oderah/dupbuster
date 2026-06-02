#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBCheckpointStore.h"
#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBGrouper.h"
#import "DBHashPipeline.h"
#import "DBHashSettings.h"
#import "DBIndexWriter.h"
#import "DBScanOrchestrator.h"
#import "DBScanPhase.h"
#import "DBScanProgressBridge.h"
#import "DBScanRunStatus.h"
#import "DBScanRunSnapshot.h"
#import "DBScanStartRequest.h"
#import "DBSizeBucketIndex.h"
#import "DBStagedFile.h"
#import "DBProductionScanDiscoveryRunner.h"
#import "DBToctouStatVerifier.h"
#import "DBFileStatReading.h"

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

@end
