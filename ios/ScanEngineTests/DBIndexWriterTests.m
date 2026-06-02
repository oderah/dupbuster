#import <XCTest/XCTest.h>
#import <sqlite3.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogSchema.h"
#import "DBDiscoveredEntry.h"
#import "DBHashedFile.h"
#import "DBIndexWriter.h"
#import "DBNormalizationProfile.h"
#import "DBStagedFile.h"
#import "DBFileStat.h"
#import "DBUnscannableReason.h"

@interface DBIndexWriterTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBIndexWriterTests

- (void)setUp
{
  [super setUp];
  self.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([self.database openWithError:&error], @"%@", error);
  self.writer = [[DBIndexWriter alloc] initWithDatabase:self.database];
  self.rootId = [self.writer insertScanRootWithUriOrGrant:@"file:///docs"
                                                     mode:@"user_selected"
                                           platformReason:nil];
}

- (void)tearDown
{
  [self.database close];
  [super tearDown];
}

- (void)testReadCatalogMeta_returnsSchemaVersion
{
  DBCatalogMeta *meta = [self.writer readCatalogMeta];
  XCTAssertEqual(meta.schemaVersion, DBCatalogSchemaCurrentVersion);
  XCTAssertFalse(meta.fullRescanRequired);
}

- (void)testUpsertHashed_createsFingerprintAndFileEntry
{
  DBStagedFile *staged = [self stagedWithUri:@"file:///a.bin" sizeBytes:100];
  DBHashedFile *hashed =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:@"abc123"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  NSInteger fileEntryId = [self.writer upsertHashedFile:hashed generation:1];
  XCTAssertGreaterThan(fileEntryId, 0);
  XCTAssertEqual([self.writer fileEntryCount], 1);
}

- (void)testHardLink_sameInodeDevice_oneEntryOneAlias
{
  DBStagedFile *stagedA = [self stagedWithUri:@"file:///a.txt"
                                     sizeBytes:100
                                         inode:@99
                                      deviceId:@7];
  DBStagedFile *stagedB = [self stagedWithUri:@"file:///b.txt"
                                     sizeBytes:100
                                         inode:@99
                                      deviceId:@7];
  DBHashedFile *hashedA =
      [[DBHashedFile alloc] initWithStaged:stagedA
                                 hashValue:@"same"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  DBHashedFile *hashedB =
      [[DBHashedFile alloc] initWithStaged:stagedB
                                 hashValue:@"same"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  [self.writer upsertHashedFile:hashedA generation:1];
  [self.writer upsertHashedFile:hashedB generation:1];
  XCTAssertEqual([self.writer fileEntryCount], 1);
  XCTAssertEqual([self aliasCount], 1);
}

- (void)testTombstoneDeletedMidHash_existingEntry_leavesGenerationUnchanged
{
  DBStagedFile *staged = [self stagedWithUri:@"file:///vanished.bin" sizeBytes:50];
  DBHashedFile *hashed =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:@"h-vanished"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  [self.writer upsertHashedFile:hashed generation:1];
  NSInteger fileEntryId = [self.writer tombstoneDeletedMidHashWithStaged:staged currentGeneration:2];
  XCTAssertGreaterThan(fileEntryId, 0);
  XCTAssertEqual([self lastSeenGenerationForFileEntryId:fileEntryId], 1);
  XCTAssertEqual([self.writer purgeEntriesNotSeenInGeneration:self.rootId generation:2], 1);
  XCTAssertEqual([self.writer fileEntryCount], 0);
}

- (void)testTombstoneDeletedMidHash_noPriorEntry_returnsZero
{
  DBStagedFile *staged = [self stagedWithUri:@"file:///never-indexed.bin" sizeBytes:10];
  XCTAssertEqual([self.writer tombstoneDeletedMidHashWithStaged:staged currentGeneration:1], 0);
  XCTAssertEqual([self.writer fileEntryCount], 0);
}

- (DBStagedFile *)stagedWithUri:(NSString *)uri
                      sizeBytes:(int64_t)sizeBytes
                          inode:(nullable NSNumber *)inode
                       deviceId:(nullable NSNumber *)deviceId
{
  NSURL *url = [NSURL URLWithString:uri];
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:url
                             phAssetLocalIdentifier:nil
                                         scanRootId:self.rootId
                                         generation:1
                                        displayName:@"file"
                                      mediaTypeHint:DBMediaTypeHintOther
                                          sizeBytes:sizeBytes
                                            mtimeNs:1000];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:sizeBytes
                                    mtimeNs:1000
                                      inode:inode
                                   deviceId:deviceId
                                 isSymlink:NO];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

- (DBStagedFile *)stagedWithUri:(NSString *)uri sizeBytes:(int64_t)sizeBytes
{
  return [self stagedWithUri:uri sizeBytes:sizeBytes inode:nil deviceId:nil];
}

- (void)testUpsertVideoPartialHashed_persistsVideoDecodeFailedReason
{
  DBStagedFile *staged = [self stagedWithUri:@"file:///broken.mp4" sizeBytes:100];
  DBHashedFile *raw =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:@"rawonly"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil
                          frameHashesBlob:nil];
  NSInteger fileEntryId = [self.writer upsertVideoPartialHashed:raw
                                         videoUnscannableReason:DBUnscannableReasonVideoDecodeFailed
                                                   generation:1];
  XCTAssertGreaterThan(fileEntryId, 0);
  XCTAssertEqualObjects([self unscannableReasonForFileEntryId:fileEntryId],
                        DBUnscannableReasonVideoDecodeFailed);
}

- (NSString *)unscannableReasonForFileEntryId:(NSInteger)fileEntryId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3_prepare_v2(
      self.database.db, "SELECT unscannable_reason FROM file_entry WHERE id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, fileEntryId);
  NSString *reason = nil;
  if (sqlite3_step(stmt) == SQLITE_ROW && sqlite3_column_type(stmt, 0) != SQLITE_NULL) {
    reason = @(sqlite3_column_text(stmt, 0));
  }
  sqlite3_finalize(stmt);
  return reason;
}

- (NSInteger)lastSeenGenerationForFileEntryId:(NSInteger)fileEntryId
{
  sqlite3_stmt *stmt = NULL;
  sqlite3_prepare_v2(
      self.database.db, "SELECT last_seen_generation FROM file_entry WHERE id = ?", -1, &stmt, NULL);
  sqlite3_bind_int64(stmt, 1, fileEntryId);
  NSInteger generation = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    generation = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return generation;
}

- (NSInteger)aliasCount
{
  sqlite3_stmt *stmt = NULL;
  sqlite3_prepare_v2(self.database.db, "SELECT COUNT(*) FROM file_path", -1, &stmt, NULL);
  NSInteger count = 0;
  if (sqlite3_step(stmt) == SQLITE_ROW) {
    count = sqlite3_column_int(stmt, 0);
  }
  sqlite3_finalize(stmt);
  return count;
}

@end
