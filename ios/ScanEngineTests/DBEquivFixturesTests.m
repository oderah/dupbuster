#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBGrouper.h"
#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBGrayFrame.h"
#import "DBHashPipeline.h"
#import "DBHashSettings.h"
#import "DBHashedFile.h"
#import "DBInMemoryDurationBucketIndex.h"
#import "DBInMemorySizeBucketIndex.h"
#import "DBIndexWriter.h"
#import "DBMatchKind.h"
#import "DBMediaTypeHintResolver.h"
#import "DBNormalizationProfile.h"
#import "DBStagedFile.h"
#import "DBUnscannableReason.h"
#import "DBVideoContentMatcher.h"
#import "DBVideoFingerprint.h"
#import "DBVideoFingerprinter.h"
#import "DBVideoFrameExtractor.h"

static NSString *DBFixtureRootPath(void)
{
  NSString *dir = [NSFileManager defaultManager].currentDirectoryPath;
  while (dir.length > 0) {
    NSString *candidate = [dir stringByAppendingPathComponent:@"tests/fixtures/dupbuster/v1/manifest.json"];
    if ([[NSFileManager defaultManager] fileExistsAtPath:candidate]) {
      return [dir stringByAppendingPathComponent:@"tests/fixtures/dupbuster/v1"];
    }
    dir = [dir stringByDeletingLastPathComponent];
  }
  XCTFail(@"Could not locate tests/fixtures/dupbuster/v1");
  return @"";
}

static NSDictionary *DBLoadFixture(NSString *relativePath)
{
  NSString *path = [DBFixtureRootPath() stringByAppendingPathComponent:relativePath];
  NSData *data = [NSData dataWithContentsOfFile:path];
  XCTAssertNotNil(data, @"missing fixture %@", relativePath);
  id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
  XCTAssertTrue([json isKindOfClass:[NSDictionary class]]);
  return json;
}

static NSArray<NSString *> *DBEquivalenceFixturePaths(void)
{
  NSDictionary *manifest = DBLoadFixture(@"manifest.json");
  NSMutableArray<NSString *> *paths = [NSMutableArray array];
  for (NSDictionary *row in manifest[@"fixtures"]) {
    if ([row[@"kind"] isEqualToString:@"equivalence"]) {
      [paths addObject:row[@"path"]];
    }
  }
  [paths sortUsingSelector:@selector(compare:)];
  return paths;
}

@interface DBEquivCatalogHarness : NSObject
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBGrouper *grouper;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBEquivCatalogHarness

+ (instancetype)freshHarness
{
  DBEquivCatalogHarness *harness = [[DBEquivCatalogHarness alloc] init];
  harness.database = [DBCatalogDatabase inMemoryDatabase];
  NSError *error = nil;
  XCTAssertTrue([harness.database openWithError:&error], @"%@", error);
  harness.writer = [[DBIndexWriter alloc] initWithDatabase:harness.database];
  harness.grouper = [[DBGrouper alloc] initWithDatabase:harness.database];
  harness.rootId = [harness.writer insertScanRootWithUriOrGrant:@"file:///docs"
                                                             mode:@"user_selected"
                                                   platformReason:nil];
  return harness;
}

@end

@interface DBEquivPayloadReader : NSObject <DBFileContentReading>
@property (nonatomic, strong) NSData *payload;
@property (nonatomic, assign) int64_t logicalSizeBytes;
@property (nonatomic, assign) BOOL mustNotOpen;
@end

@implementation DBEquivPayloadReader

- (NSInputStream *)openReadForFileURL:(NSURL *)fileURL
{
  (void)fileURL;
  if (self.mustNotOpen) {
    XCTFail(@"must not open symlink for read");
  }
  return [NSInputStream inputStreamWithData:self.payload];
}

- (NSData *)readRangeForFileURL:(NSURL *)fileURL offset:(int64_t)offset length:(NSUInteger)length
{
  (void)fileURL;
  if (self.mustNotOpen) {
    XCTFail(@"must not read symlink range");
  }
  if (offset >= self.logicalSizeBytes) {
    return [NSData data];
  }
  int64_t end = MIN(offset + (int64_t)length, self.logicalSizeBytes);
  NSUInteger sliceLen = (NSUInteger)(end - offset);
  NSUInteger start = (NSUInteger)MIN(offset, (int64_t)self.payload.length);
  NSUInteger available = self.payload.length - start;
  NSUInteger copyLen = MIN(sliceLen, available);
  NSMutableData *out = [NSMutableData dataWithLength:sliceLen];
  if (copyLen > 0) {
    [out replaceBytesInRange:NSMakeRange(0, copyLen) withBytes:self.payload.bytes + start];
  }
  return out;
}

@end

@interface DBEquivDecodeFailExtractor : NSObject <DBVideoFrameExtracting>
@end

@implementation DBEquivDecodeFailExtractor

- (DBVideoFrameExtractOutcome)extractFramesForURL:(NSURL *)fileURL
                                   samplePositions:(NSArray<NSNumber *> *)samplePositions
                                         deadline:(NSDate *)deadline
                                           frames:(NSArray<DBGrayFrame *> * _Nullable *)frames
                                       durationMs:(int64_t *)durationMs
                                       videoWidth:(NSInteger *)videoWidth
                                      videoHeight:(NSInteger *)videoHeight
{
  (void)fileURL;
  (void)samplePositions;
  (void)deadline;
  (void)frames;
  (void)durationMs;
  (void)videoWidth;
  (void)videoHeight;
  return DBVideoFrameExtractOutcomeDecodeFailed;
}

@end

@interface DBEquivFixturesTests : XCTestCase
@property (nonatomic, assign) int64_t inodeCounter;
@end

@implementation DBEquivFixturesTests

- (void)setUp
{
  [super setUp];
  self.inodeCounter = 1;
}

- (void)testAllEquivalenceFixtures_matchExpect
{
  for (NSString *path in DBEquivalenceFixturePaths()) {
    NSDictionary *fixture = DBLoadFixture(path);
    [self runFixture:fixture path:path];
  }
}

- (void)runFixture:(NSDictionary *)fixture path:(NSString *)path
{
  self.inodeCounter = 1;
  NSString *fixtureId = fixture[@"id"];
  if ([fixtureId hasPrefix:@"equiv-text-"] || [fixtureId isEqualToString:@"equiv-binary-01"]) {
    [self runPayloadHashFixture:fixture];
  } else if ([fixtureId isEqualToString:@"equiv-empty-01"]) {
    [self runEmptyFixture:fixture];
  } else if ([fixtureId isEqualToString:@"equiv-symlink-01"]) {
    [self runSymlinkFixture:fixture];
  } else if ([fixtureId hasPrefix:@"equiv-doc-"] || [fixtureId hasPrefix:@"equiv-img-"] ||
             [fixtureId isEqualToString:@"equiv-av-01"]) {
    [self runGrouperHashFixture:fixture];
  } else if ([fixtureId hasPrefix:@"equiv-video-xres-"]) {
    [self runVideoXresFixture:fixture];
  } else {
    XCTFail(@"%@: unhandled equivalence fixture %@", path, fixtureId);
  }
}

- (DBStagedFile *)stagedWithSuffix:(NSString *)suffix
                         sizeBytes:(int64_t)sizeBytes
                     mediaTypeHint:(NSString *)mediaTypeHint
                         isSymlink:(BOOL)isSymlink
                        durationMs:(int64_t)durationMs
                            rootId:(NSInteger)rootId
{
  NSURL *file = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:suffix]];
  DBDiscoveredEntry *discovered =
      [[DBDiscoveredEntry alloc] initWithContentURL:file
                             phAssetLocalIdentifier:nil
                                        scanRootId:rootId
                                        generation:1
                                       displayName:suffix
                                     mediaTypeHint:mediaTypeHint
                                         sizeBytes:sizeBytes
                                           mtimeNs:0];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:sizeBytes
                                    mtimeNs:0
                                      inode:@(self.inodeCounter++)
                                   deviceId:@1
                                  isSymlink:isSymlink
                                 durationMs:durationMs
                                 videoWidth:0
                                videoHeight:0];
  return [[DBStagedFile alloc] initWithDiscovered:discovered fileStat:stat];
}

- (DBEquivPayloadReader *)readerForPayload:(NSData *)payload logicalSize:(int64_t)logicalSize
{
  DBEquivPayloadReader *reader = [[DBEquivPayloadReader alloc] init];
  reader.payload = payload;
  reader.logicalSizeBytes = logicalSize;
  return reader;
}

- (void)runPayloadHashFixture:(NSDictionary *)fixture
{
  NSDictionary *input = fixture[@"input"];
  NSDictionary *expect = fixture[@"expect"];
  NSString *mediaTypeHint = input[@"mediaTypeHint"] ?: DBMediaTypeHintOther;
  DBInMemorySizeBucketIndex *index = [[DBInMemorySizeBucketIndex alloc] init];
  NSMutableArray<NSString *> *hashes = [NSMutableArray array];

  for (NSUInteger i = 0; i < [input[@"files"] count]; i++) {
    NSData *data = [input[@"files"][i][@"payloadUtf8"] dataUsingEncoding:NSUTF8StringEncoding];
    DBHashPipeline *pipeline =
        [[DBHashPipeline alloc] initWithFileContentReader:[self readerForPayload:data logicalSize:data.length]
                                          sizeBucketIndex:index];
    DBStagedFile *staged =
        [self stagedWithSuffix:[NSString stringWithFormat:@"f%lu", (unsigned long)i]
                     sizeBytes:(int64_t)data.length
                 mediaTypeHint:mediaTypeHint
                     isSymlink:NO
                    durationMs:0
                        rootId:1];
    DBHashPipelineResult *result = [pipeline hashStagedFile:staged settings:[DBHashSettings defaultSettings]];
    XCTAssertEqual(result.outcome, DBHashPipelineOutcomeSuccess);
    [hashes addObject:result.hashed.hashValue];
    if (i == 0 && expect[@"normalizationProfile"]) {
      XCTAssertEqualObjects(result.hashed.normalizationProfile, expect[@"normalizationProfile"]);
    }
    if (i == 0 && expect[@"hashValue"]) {
      XCTAssertEqualObjects(result.hashed.hashValue, expect[@"hashValue"]);
    }
  }

  if (expect[@"hashValues"]) {
    XCTAssertEqualObjects(hashes, expect[@"hashValues"]);
  }

  [self assertSameDuplicateGroupWithExpect:expect
                                  hashes:hashes
                                 profile:expect[@"normalizationProfile"] ?: DBNormalizationProfileRawBytes];
}

- (void)runEmptyFixture:(NSDictionary *)fixture
{
  NSDictionary *input = fixture[@"input"];
  NSDictionary *expect = fixture[@"expect"];
  DBInMemorySizeBucketIndex *index = [[DBInMemorySizeBucketIndex alloc] init];
  NSMutableArray<NSString *> *hashes = [NSMutableArray array];

  for (NSInteger i = 0; i < [input[@"fileCount"] integerValue]; i++) {
    DBHashPipeline *pipeline =
        [[DBHashPipeline alloc] initWithFileContentReader:[self readerForPayload:[NSData data] logicalSize:0]
                                          sizeBucketIndex:index];
    DBStagedFile *staged =
        [self stagedWithSuffix:[NSString stringWithFormat:@"z%ld", (long)i]
                     sizeBytes:0
                 mediaTypeHint:DBMediaTypeHintOther
                     isSymlink:NO
                    durationMs:0
                        rootId:1];
    DBHashPipelineResult *result = [pipeline hashStagedFile:staged settings:[DBHashSettings defaultSettings]];
    XCTAssertEqualObjects(result.hashed.hashValue, expect[@"hashValue"]);
    [hashes addObject:result.hashed.hashValue];
  }

  [self assertSameDuplicateGroupWithExpect:expect hashes:hashes profile:DBNormalizationProfileEmpty];
}

- (void)runSymlinkFixture:(NSDictionary *)fixture
{
  NSDictionary *expect = fixture[@"expect"];
  DBEquivPayloadReader *reader = [[DBEquivPayloadReader alloc] init];
  reader.mustNotOpen = YES;
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:reader
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];
  DBStagedFile *staged =
      [self stagedWithSuffix:@"link"
                   sizeBytes:10
               mediaTypeHint:DBMediaTypeHintOther
                   isSymlink:YES
                  durationMs:0
                      rootId:1];
  DBHashPipelineResult *result = [pipeline hashStagedFile:staged settings:[DBHashSettings defaultSettings]];
  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeSymlinkNode);
  XCTAssertFalse([expect[@"sameDuplicateGroup"] boolValue]);
}

- (void)runGrouperHashFixture:(NSDictionary *)fixture
{
  NSDictionary *input = fixture[@"input"];
  NSDictionary *expect = fixture[@"expect"];
  NSString *profile = expect[@"normalizationProfile"] ?: DBNormalizationProfileRawBytes;
  DBEquivCatalogHarness *catalog = [DBEquivCatalogHarness freshHarness];

  for (NSUInteger i = 0; i < [input[@"files"] count]; i++) {
    NSString *hash = input[@"files"][i][@"hashValue"];
    DBStagedFile *staged =
        [self stagedWithSuffix:[NSString stringWithFormat:@"g%lu", (unsigned long)i]
                     sizeBytes:100
                 mediaTypeHint:DBMediaTypeHintOther
                     isSymlink:NO
                    durationMs:0
                        rootId:catalog.rootId];
    DBHashedFile *hashed =
        [[DBHashedFile alloc] initWithStaged:staged
                                   hashValue:hash
                        normalizationProfile:profile
                             quickSampleHash:nil
                            frameHashesBlob:nil];
    [catalog.writer upsertHashedFile:hashed generation:1];
  }

  [catalog.grouper rebuildDuplicateGroups];
  [self assertGrouperDuplicateGroupsWithExpect:expect catalog:catalog];
}

- (void)runVideoXresFixture:(NSDictionary *)fixture
{
  NSString *fixtureId = fixture[@"id"];
  if ([fixtureId isEqualToString:@"equiv-video-xres-09"]) {
    [self runVideoDecodeFailedFixture:fixture];
  } else if ([fixtureId isEqualToString:@"equiv-video-xres-10"]) {
    [self runVideoBudgetSkipFixture:fixture];
  } else if ([fixtureId isEqualToString:@"equiv-video-xres-04"]) {
    [self runVideoExactBytesPrecedenceFixture:fixture];
  } else if ([fixtureId isEqualToString:@"equiv-video-xres-05"] ||
             [fixtureId isEqualToString:@"equiv-video-xres-05b"]) {
    [self runVideoDurationGateFixture:fixture];
  } else {
    [self runVideoMatcherFixture:fixture];
  }
}

- (NSArray<DBVideoFingerprint *> *)videoMembersFromInput:(NSDictionary *)input
{
  NSMutableArray<DBVideoFingerprint *> *members = [NSMutableArray array];
  for (NSDictionary *row in input[@"members"]) {
    [members addObject:[[DBVideoFingerprint alloc] initWithFrameHashes:row[@"frameHashes"]
                                                           durationMs:[row[@"durationMs"] longLongValue]
                                                           videoWidth:1920
                                                          videoHeight:1080]];
  }
  return members;
}

- (void)runVideoMatcherFixture:(NSDictionary *)fixture
{
  NSDictionary *expect = fixture[@"expect"];
  NSArray<DBVideoFingerprint *> *members = [self videoMembersFromInput:fixture[@"input"]];

  if (expect[@"contentMatchesAllPairs"]) {
    BOOL allPairs = [expect[@"contentMatchesAllPairs"] boolValue];
    for (NSUInteger i = 0; i < members.count; i++) {
      for (NSUInteger j = i + 1; j < members.count; j++) {
        BOOL matches = [DBVideoContentMatcher contentMatchesLeft:members[i] right:members[j] hammingThreshold:8];
        XCTAssertEqual(matches, allPairs);
      }
    }
  }

  if (expect[@"contentMatches"]) {
    BOOL expected = [expect[@"contentMatches"] boolValue];
    BOOL actual = [DBVideoContentMatcher contentMatchesLeft:members[0] right:members[1] hammingThreshold:8];
    XCTAssertEqual(actual, expected);
  }
}

- (void)runVideoDurationGateFixture:(NSDictionary *)fixture
{
  NSArray<DBVideoFingerprint *> *members = [self videoMembersFromInput:fixture[@"input"]];
  BOOL passes = [DBVideoContentMatcher passesDurationGateWithDurationA:members[0].durationMs
                                                             durationB:members[1].durationMs];
  XCTAssertEqual(passes, [fixture[@"expect"][@"passesDurationGate"] boolValue]);
}

- (void)runVideoExactBytesPrecedenceFixture:(NSDictionary *)fixture
{
  NSDictionary *input = fixture[@"input"];
  NSDictionary *expect = fixture[@"expect"];
  NSString *rawHash = input[@"rawHash"];
  NSArray *frames = input[@"members"][0][@"frameHashes"];
  NSString *videoHash = [[frames valueForKey:@"description"] componentsJoinedByString:@"-"];
  DBEquivCatalogHarness *catalog = [DBEquivCatalogHarness freshHarness];

  for (NSInteger i = 0; i < 2; i++) {
    DBStagedFile *staged =
        [self stagedWithSuffix:[NSString stringWithFormat:@"v%ld", (long)i]
                     sizeBytes:100
                 mediaTypeHint:DBMediaTypeHintVideo
                     isSymlink:NO
                    durationMs:60000
                        rootId:catalog.rootId];
    DBHashedFile *raw =
        [[DBHashedFile alloc] initWithStaged:staged hashValue:rawHash normalizationProfile:DBNormalizationProfileRawBytes quickSampleHash:nil frameHashesBlob:nil];
    DBHashedFile *video =
        [[DBHashedFile alloc] initWithStaged:staged hashValue:videoHash normalizationProfile:DBNormalizationProfileVideoContentV1 quickSampleHash:nil frameHashesBlob:nil];
    DBHashPipelineResult *result = [[DBHashPipelineResult alloc] init];
    result.outcome = DBHashPipelineOutcomeVideoSuccess;
    result.rawBytesHashed = raw;
    result.videoContentHashed = video;
    [catalog.writer persistHashPipelineResult:result staged:staged generation:1];
  }

  [catalog.grouper rebuildDuplicateGroups];
  XCTAssertEqual([catalog.grouper duplicateGroupCount], 1);
  XCTAssertEqualObjects([catalog.grouper matchKindForGroupId:[catalog.grouper firstDuplicateGroupId]],
                        expect[@"matchKind"]);
}

- (void)runVideoDecodeFailedFixture:(NSDictionary *)fixture
{
  DBVideoFingerprinter *fingerprinter =
      [[DBVideoFingerprinter alloc] initWithFrameExtractor:[[DBEquivDecodeFailExtractor alloc] init]];
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:[self readerForPayload:[NSData dataWithBytes:"\0" length:1] logicalSize:1]
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]
                                   durationBucketIndex:[[DBInMemoryDurationBucketIndex alloc] init]
                                     videoFingerprinter:fingerprinter];
  DBStagedFile *staged =
      [self stagedWithSuffix:@"bad.mp4"
                   sizeBytes:1024
               mediaTypeHint:DBMediaTypeHintVideo
                   isSymlink:NO
                  durationMs:10000
                      rootId:1];
  DBHashPipelineResult *result = [pipeline hashStagedFile:staged settings:[DBHashSettings defaultSettings]];
  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeVideoPartialSuccess);
  XCTAssertEqualObjects(result.unscannableReason, fixture[@"expect"][@"unscannableReason"]);
}

- (void)runVideoBudgetSkipFixture:(NSDictionary *)fixture
{
  NSDictionary *input = fixture[@"input"];
  int64_t sizeBytes = [input[@"sizeBytes"] longLongValue];
  DBHashSettings *settings = [DBHashSettings defaultSettings];
  settings.largeFilesOptIn = [input[@"largeFilesOptIn"] boolValue];
  DBVideoFingerprinter *fingerprinter =
      [[DBVideoFingerprinter alloc] initWithFrameExtractor:[[DBEquivDecodeFailExtractor alloc] init]];
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:[self readerForPayload:[NSData dataWithBytes:"\1" length:1] logicalSize:sizeBytes]
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]
                                   durationBucketIndex:[[DBInMemoryDurationBucketIndex alloc] init]
                                     videoFingerprinter:fingerprinter];
  DBStagedFile *staged =
      [self stagedWithSuffix:@"big.mp4"
                   sizeBytes:sizeBytes
               mediaTypeHint:DBMediaTypeHintVideo
                   isSymlink:NO
                  durationMs:60000
                      rootId:1];
  DBHashPipelineResult *result = [pipeline hashStagedFile:staged settings:settings];
  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeVideoPartialSuccess);
  XCTAssertTrue([fixture[@"expect"][@"videoContentFingerprintSkipped"] boolValue]);
}

- (void)assertSameDuplicateGroupWithExpect:(NSDictionary *)expect
                                    hashes:(NSArray<NSString *> *)hashes
                                   profile:(NSString *)profile
{
  if (!expect[@"sameDuplicateGroup"]) {
    return;
  }
  BOOL sameGroup = [expect[@"sameDuplicateGroup"] boolValue];
  NSSet *unique = [NSSet setWithArray:hashes];
  if (sameGroup) {
    XCTAssertEqual(unique.count, 1u);
  } else {
    XCTAssertTrue(unique.count > 1 || hashes.count < 2);
  }
  if (hashes.count < 2) {
    return;
  }

  DBEquivCatalogHarness *catalog = [DBEquivCatalogHarness freshHarness];
  for (NSUInteger i = 0; i < hashes.count; i++) {
    DBStagedFile *staged =
        [self stagedWithSuffix:[NSString stringWithFormat:@"d%lu", (unsigned long)i]
                     sizeBytes:100
                 mediaTypeHint:DBMediaTypeHintOther
                     isSymlink:NO
                    durationMs:0
                        rootId:catalog.rootId];
    DBHashedFile *hashed =
        [[DBHashedFile alloc] initWithStaged:staged
                                   hashValue:hashes[i]
                        normalizationProfile:profile
                             quickSampleHash:nil
                            frameHashesBlob:nil];
    [catalog.writer upsertHashedFile:hashed generation:1];
  }
  [catalog.grouper rebuildDuplicateGroups];
  [self assertGrouperDuplicateGroupsWithExpect:expect catalog:catalog];
}

- (void)assertGrouperDuplicateGroupsWithExpect:(NSDictionary *)expect catalog:(DBEquivCatalogHarness *)catalog
{
  if (!expect[@"sameDuplicateGroup"]) {
    return;
  }
  BOOL sameGroup = [expect[@"sameDuplicateGroup"] boolValue];
  if (sameGroup) {
    XCTAssertEqual([catalog.grouper duplicateGroupCount], 1);
  } else {
    XCTAssertEqual([catalog.grouper duplicateGroupCount], 0);
  }
}

@end
