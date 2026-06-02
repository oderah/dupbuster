#import <XCTest/XCTest.h>

#import "DBDiscoveredEntry.h"
#import "DBFileStat.h"
#import "DBStagedFile.h"
#import "DBToctouStatVerifier.h"

@interface DBFakeFileStatReader : NSObject <DBFileStatReading>
@property (nonatomic, strong) DBFileStat *statToReturn;
@property (nonatomic, assign) BOOL returnNil;
@end

@implementation DBFakeFileStatReader

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error
{
  (void)fileURL;
  (void)error;
  return self.returnNil ? nil : self.statToReturn;
}

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error
{
  (void)localIdentifier;
  (void)error;
  return self.returnNil ? nil : self.statToReturn;
}

@end

@interface DBToctouStatVerifierTests : XCTestCase
@end

@implementation DBToctouStatVerifierTests

- (void)testVerifyBaseline_matchingMetadata_returnsConsistent
{
  DBStagedFile *staged = [self stagedWithSize:100 mtimeNs:2000];
  DBFakeFileStatReader *reader = [[DBFakeFileStatReader alloc] init];
  reader.statToReturn =
      [[DBFileStat alloc] initWithSizeBytes:100
                                    mtimeNs:2000
                                      inode:nil
                                   deviceId:nil
                                  isSymlink:NO
                                 durationMs:0
                                 videoWidth:0
                                videoHeight:0];

  DBToctouStatVerifier *verifier = [[DBToctouStatVerifier alloc] initWithFileStatReader:reader];
  DBFileStat *fresh = nil;
  XCTAssertEqual([verifier verifyBaselineForStaged:staged freshStat:&fresh], DBToctouVerifyOutcomeConsistent);
  XCTAssertNil(fresh);
}

- (void)testVerifyBaseline_sizeChanged_returnsChanged
{
  DBStagedFile *staged = [self stagedWithSize:100 mtimeNs:2000];
  DBFakeFileStatReader *reader = [[DBFakeFileStatReader alloc] init];
  reader.statToReturn =
      [[DBFileStat alloc] initWithSizeBytes:200
                                    mtimeNs:2000
                                      inode:nil
                                   deviceId:nil
                                  isSymlink:NO
                                 durationMs:0
                                 videoWidth:0
                                videoHeight:0];

  DBToctouStatVerifier *verifier = [[DBToctouStatVerifier alloc] initWithFileStatReader:reader];
  DBFileStat *fresh = nil;
  XCTAssertEqual([verifier verifyBaselineForStaged:staged freshStat:&fresh], DBToctouVerifyOutcomeChanged);
  XCTAssertEqual(fresh.sizeBytes, 200);
}

- (DBStagedFile *)stagedWithSize:(int64_t)size mtimeNs:(int64_t)mtimeNs
{
  NSURL *url = [NSURL URLWithString:@"file:///tmp/toctou.bin"];
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:url
                             phAssetLocalIdentifier:nil
                                         scanRootId:1
                                         generation:1
                                        displayName:@"toctou.bin"
                                      mediaTypeHint:DBMediaTypeHintOther
                                          sizeBytes:size
                                            mtimeNs:mtimeNs];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:size
                                    mtimeNs:mtimeNs
                                      inode:nil
                                   deviceId:nil
                                  isSymlink:NO
                                 durationMs:0
                                 videoWidth:0
                                videoHeight:0];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

@end
