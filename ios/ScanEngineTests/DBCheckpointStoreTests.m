#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBCheckpointStore.h"
#import "DBIndexWriter.h"
#import "DBScanRunSnapshot.h"
#import "DBScanRunStatus.h"

@interface DBCheckpointStoreTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBCheckpointStore *checkpoints;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBCheckpointStoreTests

- (void)setUp
{
  [super setUp];
  self.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([self.database openWithError:&error], @"%@", error);
  self.writer = [[DBIndexWriter alloc] initWithDatabase:self.database];
  self.checkpoints = [[DBCheckpointStore alloc] initWithDatabase:self.database];
  self.rootId = [self.writer insertScanRootWithUriOrGrant:@"file:///docs"
                                                     mode:@"user_selected"
                                           platformReason:nil];
}

- (void)tearDown
{
  [self.database close];
  [super tearDown];
}

- (void)testBeginRun_createsRunningRunWithZeroCheckpoint
{
  NSError *error = nil;
  NSInteger runId = [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  XCTAssertNil(error);
  XCTAssertGreaterThan(runId, 0);

  DBScanRunSnapshot *snapshot = [self.checkpoints runWithId:runId];
  XCTAssertEqualObjects(snapshot.status, DBScanRunStatusRunning);
  XCTAssertEqual(snapshot.lastProcessedId, 0);
}

- (void)testSaveCheckpoint_persistsMonotonicLastProcessedId
{
  NSError *error = nil;
  NSInteger runId = [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  XCTAssertTrue([self.checkpoints saveCheckpointForRunId:runId lastProcessedId:42 error:&error]);
  XCTAssertTrue([self.checkpoints saveCheckpointForRunId:runId lastProcessedId:100 error:&error]);

  XCTAssertEqual([self.checkpoints runWithId:runId].lastProcessedId, 100);
}

- (void)testFindResumableRun_afterProcessKill
{
  NSError *error = nil;
  NSInteger runId = [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  [self.checkpoints saveCheckpointForRunId:runId lastProcessedId:500 error:&error];

  DBScanRunSnapshot *resumable = [self.checkpoints findResumableRun];
  XCTAssertNotNil(resumable);
  XCTAssertEqual(resumable.scanRunId, runId);
  XCTAssertEqual(resumable.lastProcessedId, 500);
}

- (void)testPauseAndResumeRun
{
  NSError *error = nil;
  NSInteger runId = [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  [self.checkpoints saveCheckpointForRunId:runId lastProcessedId:77 error:&error];
  XCTAssertTrue([self.checkpoints pauseRunWithId:runId error:&error]);

  DBScanRunSnapshot *resumed = [self.checkpoints resumeRunWithId:runId error:&error];
  XCTAssertNil(error);
  XCTAssertEqualObjects(resumed.status, DBScanRunStatusRunning);
  XCTAssertEqual(resumed.lastProcessedId, 77);
}

- (void)testBeginRun_rejectsConflictingActiveRun
{
  NSError *error = nil;
  [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  NSInteger second = [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  XCTAssertNotNil(error);
  XCTAssertEqual(second, 0);
  XCTAssertEqual(error.code, DBCheckpointStoreErrorConflict);
}

- (void)testAbandonForRestart_makesRunNotResumable
{
  NSError *error = nil;
  NSInteger runId = [self.checkpoints beginRunWithRootId:self.rootId generation:1 error:&error];
  [self.checkpoints abandonForRestartRunWithId:runId
                                     endedAtMs:(int64_t)(NSDate.date.timeIntervalSince1970 * 1000)
                                         error:&error];
  XCTAssertNil([self.checkpoints findResumableRun]);
}

@end
