#import <XCTest/XCTest.h>

#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBHashConstants.h"
#import "DBHashPipeline.h"
#import "DBHashSettings.h"
#import "DBInMemorySizeBucketIndex.h"
#import "DBNormalizationProfile.h"
#import "DBUnscannableReason.h"

@interface DBFakeFileContentReader : NSObject <DBFileContentReading>
@property (nonatomic, strong) NSData *payload;
@property (nonatomic, assign) int64_t logicalSizeBytes;
@end

@implementation DBFakeFileContentReader

- (nullable NSInputStream *)openReadForFileURL:(NSURL *)fileURL
{
  (void)fileURL;
  return [NSInputStream inputStreamWithData:self.payload];
}

- (nullable NSData *)readRangeForFileURL:(NSURL *)fileURL offset:(int64_t)offset length:(NSUInteger)length
{
  (void)fileURL;
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
    [out replaceBytesInRange:NSMakeRange(0, copyLen)
                   withBytes:self.payload.bytes + start];
  }
  return out;
}

@end

@interface DBHashPipelineTests : XCTestCase
@end

@implementation DBHashPipelineTests

- (DBStagedFile *)stagedWithSize:(int64_t)sizeBytes
                   mediaTypeHint:(NSString *)mediaTypeHint
                      isSymlink:(BOOL)isSymlink
                         payload:(NSData *)payload
{
  NSURL *folder = [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
  NSURL *file = [folder URLByAppendingPathComponent:@"dupbuster-hash-test.bin"];
  DBDiscoveredEntry *discovered =
      [[DBDiscoveredEntry alloc] initWithContentURL:file
                             phAssetLocalIdentifier:nil
                                        scanRootId:1
                                        generation:1
                                       displayName:@"dupbuster-hash-test.bin"
                                     mediaTypeHint:mediaTypeHint
                                         sizeBytes:sizeBytes
                                           mtimeNs:0];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:sizeBytes
                                    mtimeNs:0
                                      inode:@1
                                   deviceId:@1
                                  isSymlink:isSymlink];
  (void)payload;
  return [[DBStagedFile alloc] initWithDiscovered:discovered fileStat:stat];
}

- (void)testHash_emptyFile_returnsEmptyProfile
{
  DBFakeFileContentReader *reader = [[DBFakeFileContentReader alloc] init];
  reader.payload = [NSData data];
  reader.logicalSizeBytes = 0;
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:reader
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];

  DBHashPipelineResult *result =
      [pipeline hashStagedFile:[self stagedWithSize:0
                                      mediaTypeHint:DBMediaTypeHintOther
                                         isSymlink:NO
                                            payload:reader.payload]
                      settings:[DBHashSettings defaultSettings]];

  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeSuccess);
  XCTAssertEqualObjects(result.hashed.hashValue, DBNormalizationProfileEmpty);
  XCTAssertEqualObjects(result.hashed.normalizationProfile, DBNormalizationProfileEmpty);
  XCTAssertNil(result.hashed.quickSampleHash);
}

- (void)testHash_smallFile_returnsRawBytesSha256
{
  NSData *payload = [@"hello" dataUsingEncoding:NSUTF8StringEncoding];
  DBFakeFileContentReader *reader = [[DBFakeFileContentReader alloc] init];
  reader.payload = payload;
  reader.logicalSizeBytes = payload.length;
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:reader
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];

  DBStagedFile *staged =
      [self stagedWithSize:payload.length mediaTypeHint:DBMediaTypeHintOther isSymlink:NO payload:payload];
  DBHashPipelineResult *result =
      [pipeline hashStagedFile:staged settings:[DBHashSettings defaultSettings]];

  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeSuccess);
  XCTAssertEqualObjects(result.hashed.normalizationProfile, DBNormalizationProfileRawBytes);
  XCTAssertEqualObjects(
      result.hashed.hashValue,
      @"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
  XCTAssertNil(result.hashed.quickSampleHash);
}

- (void)testHash_uniqueSize_skipsByteRead
{
  NSData *payload = [NSData dataWithBytes:"\x01" length:1];
  DBFakeFileContentReader *reader = [[DBFakeFileContentReader alloc] init];
  reader.payload = payload;
  reader.logicalSizeBytes = 1024;
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:reader
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];

  DBHashPipelineResult *result =
      [pipeline hashStagedFile:[self stagedWithSize:1024
                                      mediaTypeHint:DBMediaTypeHintOther
                                         isSymlink:NO
                                            payload:payload]
                      settings:[DBHashSettings defaultSettings]];

  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeSizeBucketSkipped);
}

- (void)testHash_videoUniqueSize_stillHashes
{
  NSData *payload = [NSData dataWithBytes:"\x01" length:1];
  DBFakeFileContentReader *reader = [[DBFakeFileContentReader alloc] init];
  reader.payload = payload;
  reader.logicalSizeBytes = 1;
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:reader
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];

  DBHashPipelineResult *result =
      [pipeline hashStagedFile:[self stagedWithSize:1
                                      mediaTypeHint:DBMediaTypeHintVideo
                                         isSymlink:NO
                                            payload:payload]
                      settings:[DBHashSettings defaultSettings]];

  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeSuccess);
}

- (void)testHash_largeFileWithoutOptIn_returnsLargeSkipped
{
  DBFakeFileContentReader *reader = [[DBFakeFileContentReader alloc] init];
  reader.payload = [NSData dataWithBytes:"\x01" length:1];
  reader.logicalSizeBytes = DBHashLargeFileCapBytes + 1;
  DBHashPipeline *pipeline =
      [[DBHashPipeline alloc] initWithFileContentReader:reader
                                        sizeBucketIndex:[[DBInMemorySizeBucketIndex alloc] init]];

  DBHashPipelineResult *result =
      [pipeline hashStagedFile:[self stagedWithSize:DBHashLargeFileCapBytes + 1
                                      mediaTypeHint:DBMediaTypeHintOther
                                         isSymlink:NO
                                            payload:reader.payload]
                      settings:[DBHashSettings defaultSettings]];

  XCTAssertEqual(result.outcome, DBHashPipelineOutcomeUnscannable);
  XCTAssertEqualObjects(result.unscannableReason, DBUnscannableReasonLargeSkipped);
}

@end
