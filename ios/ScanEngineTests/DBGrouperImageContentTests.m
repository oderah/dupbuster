#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBGrouper.h"
#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBHashedFile.h"
#import "DBImageFingerprint.h"
#import "DBIndexWriter.h"
#import "DBMatchKind.h"
#import "DBNormalizationProfile.h"
#import "DBStagedFile.h"

@interface DBGrouperImageContentTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBGrouper *grouper;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBGrouperImageContentTests

- (void)setUp
{
  [super setUp];
  self.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([self.database openWithError:&error], @"%@", error);
  self.writer = [[DBIndexWriter alloc] initWithDatabase:self.database];
  self.grouper = [[DBGrouper alloc] initWithDatabase:self.database];
  self.rootId = [self.writer insertScanRootWithUriOrGrant:@"content://test/tree"
                                                     mode:@"user_selected"
                                           platformReason:nil];
}

- (void)tearDown
{
  [self.database close];
  [super tearDown];
}

- (void)testRebuildDuplicateGroups_mixedRawBytes_usesSameContentImageForAllThree
{
  uint64_t base = 0x0F0E0D0C0B0A0908ULL;
  uint64_t recompressed = base ^ (1ULL << 3);
  [self upsertImageDual:@"download.jpeg" rawHash:@"raw-small" dHash:recompressed];
  [self upsertImageDual:@"images.jpeg" rawHash:@"raw-large" dHash:base];
  [self upsertImageDual:@"images (2).jpeg" rawHash:@"raw-large" dHash:base];

  [self.grouper rebuildDuplicateGroups];

  NSInteger groupId = [self.grouper firstDuplicateGroupId];
  XCTAssertGreaterThan(groupId, 0);
  XCTAssertEqualObjects([self.grouper matchKindForGroupId:groupId], DBMatchKindSameContentImage);
  XCTAssertEqual([self.grouper memberCountForGroupId:groupId], 3);
  XCTAssertEqual([self.grouper duplicateGroupCount], 1);
}

- (void)testRebuildDuplicateGroups_byteIdenticalImagePair_exactBytesOnly
{
  uint64_t base = 0x0F0E0D0C0B0A0908ULL;
  [self upsertImageDual:@"a.jpg" rawHash:@"same-raw" dHash:base];
  [self upsertImageDual:@"b.jpg" rawHash:@"same-raw" dHash:base];

  [self.grouper rebuildDuplicateGroups];

  NSInteger groupId = [self.grouper firstDuplicateGroupId];
  XCTAssertGreaterThan(groupId, 0);
  XCTAssertEqualObjects([self.grouper matchKindForGroupId:groupId], DBMatchKindExactBytes);
  XCTAssertEqual([self.grouper memberCountForGroupId:groupId], 2);
}

- (void)testRebuildDuplicateGroups_fuzzyImageContent_clustersRecompressedVariants
{
  uint64_t base = 0x0F0E0D0C0B0A0908ULL;
  uint64_t closeA = base ^ (1ULL << 1);
  uint64_t closeB = base ^ (1ULL << 8);
  [self upsertImageDual:@"a.jpg" rawHash:@"raw-a" dHash:base];
  [self upsertImageDual:@"b.jpg" rawHash:@"raw-b" dHash:closeA];
  [self upsertImageDual:@"c.jpg" rawHash:@"raw-c" dHash:closeB];

  [self.grouper rebuildDuplicateGroups];

  NSInteger groupId = [self.grouper firstDuplicateGroupId];
  XCTAssertGreaterThan(groupId, 0);
  XCTAssertEqualObjects([self.grouper matchKindForGroupId:groupId], DBMatchKindSameContentImage);
  XCTAssertEqual([self.grouper memberCountForGroupId:groupId], 3);
}

- (void)upsertImageDual:(NSString *)suffix rawHash:(NSString *)rawHash dHash:(uint64_t)dHash
{
  DBStagedFile *staged = [self stagedWithSuffix:suffix];
  NSData *blob = [NSData dataWithBytes:&dHash length:sizeof(uint64_t)];
  DBHashedFile *rawBytes =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:rawHash
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil
                          frameHashesBlob:nil];
  DBHashedFile *imageContent =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:[self canonicalHashForDHash:dHash]
                      normalizationProfile:DBNormalizationProfileImageContentV1
                           quickSampleHash:nil
                          frameHashesBlob:blob];
  [self.writer upsertImageDualHashed:rawBytes imageContent:imageContent generation:1];
}

- (NSString *)canonicalHashForDHash:(uint64_t)dHash
{
  return [[[DBImageFingerprint alloc] initWithDHash:dHash] hashValue];
}

- (DBStagedFile *)stagedWithSuffix:(NSString *)suffix
{
  NSURL *url = [NSURL URLWithString:[NSString stringWithFormat:@"content://test/document/%@", suffix]];
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:url
                             phAssetLocalIdentifier:nil
                                         scanRootId:self.rootId
                                         generation:1
                                        displayName:suffix
                                      mediaTypeHint:DBMediaTypeHintImage
                                          sizeBytes:100
                                            mtimeNs:1000];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:100
                                    mtimeNs:1000
                                      inode:nil
                                   deviceId:nil
                                 isSymlink:NO];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

@end
