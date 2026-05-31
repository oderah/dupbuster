#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogReader.h"
#import "DBCatalogSnapshotBridgeMapper.h"
#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBHashedFile.h"
#import "DBGrouper.h"
#import "DBIndexWriter.h"
#import "DBMatchKind.h"
#import "DBNormalizationProfile.h"
#import "DBStagedFile.h"
#import "DBUnscannableReason.h"

@interface DBCatalogReaderTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBGrouper *grouper;
@property (nonatomic, strong) DBCatalogReader *reader;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBCatalogReaderTests

- (void)setUp
{
  [super setUp];
  self.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([self.database openWithError:&error], @"%@", error);
  self.writer = [[DBIndexWriter alloc] initWithDatabase:self.database];
  self.grouper = [[DBGrouper alloc] initWithDatabase:self.database];
  self.reader = [[DBCatalogReader alloc] initWithDatabase:self.database];
  self.rootId = [self.writer insertScanRootWithUriOrGrant:@"file:///docs"
                                                     mode:@"user_selected"
                                           platformReason:nil];
}

- (void)tearDown
{
  [self.database close];
  [super tearDown];
}

- (void)testReadSnapshot_emptyCatalog_returnsEmptyStructures
{
  DBCatalogSnapshot *snapshot = [self.reader readSnapshot];
  XCTAssertEqual(snapshot.duplicateGroups.count, 0U);
  XCTAssertEqual(snapshot.unscannableCounts.count, 0U);
  XCTAssertEqual(snapshot.groupDetailsById.count, 0U);
}

- (void)testReadSnapshot_afterGrouping_returnsGroupsMembersAndUnscannableCounts
{
  DBStagedFile *stagedA = [self stagedWithUri:@"file:///docs/a.jpg" displayName:@"photo-a.jpg" sizeBytes:100];
  DBStagedFile *stagedB = [self stagedWithUri:@"file:///docs/b.jpg" displayName:@"photo-b.jpg" sizeBytes:100];
  DBHashedFile *hashedA =
      [[DBHashedFile alloc] initWithStaged:stagedA
                                 hashValue:@"hash-a"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  DBHashedFile *hashedB =
      [[DBHashedFile alloc] initWithStaged:stagedB
                                 hashValue:@"hash-a"
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  [self.writer upsertHashedFile:hashedA generation:1];
  [self.writer upsertHashedFile:hashedB generation:1];
  [self.writer upsertUnscannableWithReason:DBUnscannableReasonHashTimeout
                                    staged:[self stagedWithUri:@"file:///docs/timeout.bin"
                                                   displayName:@"timeout.bin"
                                                     sizeBytes:50]
                                generation:1];
  [self.writer upsertUnscannableWithReason:DBUnscannableReasonLargeSkipped
                                    staged:[self stagedWithUri:@"file:///docs/large.bin"
                                                   displayName:@"large.bin"
                                                     sizeBytes:50]
                                generation:1];

  [self.grouper rebuildDuplicateGroups];

  DBCatalogSnapshot *snapshot = [self.reader readSnapshot];
  XCTAssertEqual(snapshot.duplicateGroups.count, 1U);
  DBCatalogGroupSummary *summary = snapshot.duplicateGroups.firstObject;
  XCTAssertEqualObjects(summary.matchKind, DBMatchKindExactBytes);
  XCTAssertEqual(summary.memberCount, 2);
  XCTAssertEqual(summary.reclaimableBytesEst, 100);
  XCTAssertEqual(summary.thumbnails.count, 2U);

  DBCatalogGroupDetail *detail = snapshot.groupDetailsById[@(summary.groupId)];
  XCTAssertEqual(detail.members.count, 2U);
  XCTAssertEqualObjects(detail.members.firstObject.displayName, @"photo-a.jpg");
  XCTAssertEqualObjects(detail.members.firstObject.mediaTypeHint, DBMediaTypeHintImage);
  XCTAssertNotNil(detail.members.firstObject.thumbnailUri);
  XCTAssertEqual(snapshot.unscannableCounts[DBUnscannableReasonHashTimeout].integerValue, 1);
  XCTAssertEqual(snapshot.unscannableCounts[DBUnscannableReasonLargeSkipped].integerValue, 1);
}

- (void)testBridgePayload_assertsAllowedKeys
{
  DBCatalogSnapshot *snapshot = [self.reader readSnapshot];
  NSDictionary *payload = [DBCatalogSnapshotBridgeMapper bridgePayloadForSnapshot:snapshot];
  XCTAssertEqualObjects(payload[@"duplicateGroups"], @[]);
  XCTAssertEqualObjects(payload[@"unscannableCounts"], @{});
  XCTAssertEqualObjects(payload[@"groupDetailsById"], @{});
}

- (DBStagedFile *)stagedWithUri:(NSString *)uri displayName:(NSString *)displayName sizeBytes:(int64_t)sizeBytes
{
  NSURL *url = [NSURL URLWithString:uri];
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:url
                             phAssetLocalIdentifier:nil
                                         scanRootId:self.rootId
                                         generation:1
                                        displayName:displayName
                                      mediaTypeHint:DBMediaTypeHintImage
                                          sizeBytes:sizeBytes
                                            mtimeNs:2000000000];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:sizeBytes
                                    mtimeNs:2000000000
                                      inode:nil
                                   deviceId:nil
                                 isSymlink:NO];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

@end
