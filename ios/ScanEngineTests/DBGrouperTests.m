#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBGrouper.h"
#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBHashedFile.h"
#import "DBIndexWriter.h"
#import "DBMatchKind.h"
#import "DBMediaTypeHintResolver.h"
#import "DBNormalizationProfile.h"
#import "DBStagedFile.h"
#import "DBVideoFingerprint.h"

@interface DBGrouperTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBGrouper *grouper;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBGrouperTests

- (void)setUp
{
  [super setUp];
  self.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([self.database openWithError:&error], @"%@", error);
  self.writer = [[DBIndexWriter alloc] initWithDatabase:self.database];
  self.grouper = [[DBGrouper alloc] initWithDatabase:self.database];
  self.rootId = [self.writer insertScanRootWithUriOrGrant:@"file:///docs"
                                                     mode:@"user_selected"
                                           platformReason:nil];
}

- (void)tearDown
{
  [self.database close];
  [super tearDown];
}

- (void)testRebuildDuplicateGroups_twoFilesSameFingerprint
{
  [self upsertHashed:@"a" hash:@"abc123" size:100 profile:DBNormalizationProfileRawBytes];
  [self upsertHashed:@"b" hash:@"abc123" size:200 profile:DBNormalizationProfileRawBytes];

  DBGrouperRebuildResult *result = [self.grouper rebuildDuplicateGroups];

  XCTAssertEqual(result.groupsCreated, 1);
  XCTAssertEqual([self.grouper duplicateGroupCount], 1);
  XCTAssertEqual(result.totalReclaimableBytesEst, 100);
  NSInteger groupId = [self.grouper firstDuplicateGroupId];
  XCTAssertEqual([self.grouper memberCountForGroupId:groupId], 2);
  XCTAssertEqual([self.grouper reclaimableBytesForGroupId:groupId], 100);
  XCTAssertEqualObjects([self.grouper matchKindForGroupId:groupId], DBMatchKindExactBytes);
}

- (void)testRebuildDuplicateGroups_uniqueHashes_noGroups
{
  [self upsertHashed:@"a" hash:@"h1" size:100 profile:DBNormalizationProfileRawBytes];
  [self upsertHashed:@"b" hash:@"h2" size:100 profile:DBNormalizationProfileRawBytes];

  DBGrouperRebuildResult *result = [self.grouper rebuildDuplicateGroups];

  XCTAssertEqual(result.groupsCreated, 0);
  XCTAssertEqual([self.grouper duplicateGroupCount], 0);
}

- (void)testRebuildDuplicateGroups_videoProfile_sameContentVideo
{
  uint64_t frameHash = 0x0F0E0D0C0B0A0908ULL;
  NSData *blob = [NSData dataWithBytes:&frameHash length:sizeof(uint64_t)];
  NSString *hash = [[[DBVideoFingerprint alloc] initWithFrameHashes:@[@(frameHash)]
                                                         durationMs:60000
                                                         videoWidth:1920
                                                        videoHeight:1080] hashValue];
  [self upsertHashed:@"v1.mp4" hash:hash size:100 profile:DBNormalizationProfileVideoContentV1 frameHashesBlob:blob durationMs:60000];
  [self upsertHashed:@"v2.mp4" hash:hash size:100 profile:DBNormalizationProfileVideoContentV1 frameHashesBlob:blob durationMs:60000];

  [self.grouper rebuildDuplicateGroups];

  NSInteger groupId = [self.grouper firstDuplicateGroupId];
  XCTAssertGreaterThan(groupId, 0);
  XCTAssertEqualObjects([self.grouper matchKindForGroupId:groupId], DBMatchKindSameContentVideo);
}

- (void)testEstimateReclaimableBytes
{
  XCTAssertEqual([DBGrouper estimateReclaimableBytesForSizes:@[@100]], 0);
  XCTAssertEqual([DBGrouper estimateReclaimableBytesForSizes:@[@100, @200]], 100);
  XCTAssertEqual([DBGrouper estimateReclaimableBytesForSizes:@[@100, @200, @300]], 300);
}

- (void)upsertHashed:(NSString *)suffix
                 hash:(NSString *)hash
                 size:(int64_t)size
              profile:(NSString *)profile
{
  [self upsertHashed:suffix hash:hash size:size profile:profile frameHashesBlob:nil];
}

- (void)upsertHashed:(NSString *)suffix
                 hash:(NSString *)hash
                 size:(int64_t)size
              profile:(NSString *)profile
      frameHashesBlob:(NSData *)frameHashesBlob
{
  [self upsertHashed:suffix
                 hash:hash
                 size:size
              profile:profile
      frameHashesBlob:frameHashesBlob
           durationMs:0];
}

- (void)upsertHashed:(NSString *)suffix
                 hash:(NSString *)hash
                 size:(int64_t)size
              profile:(NSString *)profile
      frameHashesBlob:(NSData *)frameHashesBlob
           durationMs:(int64_t)durationMs
{
  NSString *uri = [NSString stringWithFormat:@"file:///doc/%@", suffix];
  DBStagedFile *staged = [self stagedWithUri:uri sizeBytes:size durationMs:durationMs];
  DBHashedFile *hashed =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:hash
                      normalizationProfile:profile
                           quickSampleHash:nil
                          frameHashesBlob:frameHashesBlob];
  [self.writer upsertHashedFile:hashed generation:1];
}

- (DBStagedFile *)stagedWithUri:(NSString *)uri sizeBytes:(int64_t)sizeBytes
{
  return [self stagedWithUri:uri sizeBytes:sizeBytes durationMs:0];
}

- (DBStagedFile *)stagedWithUri:(NSString *)uri sizeBytes:(int64_t)sizeBytes durationMs:(int64_t)durationMs
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
                                      inode:nil
                                   deviceId:nil
                                 isSymlink:NO
                               durationMs:durationMs
                               videoWidth:durationMs > 0 ? 1920 : 0
                              videoHeight:durationMs > 0 ? 1080 : 0];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

@end
