#import <XCTest/XCTest.h>

#import "DBFileStat.h"
#import "DBStatStage.h"
#import "DBUriValidator.h"

@interface DBFakeFileStatReader : NSObject <DBFileStatReading>
@property (nonatomic, strong, nullable) DBFileStat *fileStat;
@property (nonatomic, strong, nullable) DBFileStat *phAssetStat;
@end

@implementation DBFakeFileStatReader

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error
{
  (void)fileURL;
  (void)error;
  return self.fileStat;
}

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error
{
  (void)localIdentifier;
  (void)error;
  return self.phAssetStat;
}

@end

@interface DBStatStageTests : XCTestCase
@end

@implementation DBStatStageTests

- (void)testStatFileURL_returnsFreshMetadata
{
  NSURL *folder =
      [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
  NSURL *file = [folder URLByAppendingPathComponent:@"dupbuster-stat-test.txt"];
  NSData *payload = [@"hello" dataUsingEncoding:NSUTF8StringEncoding];
  XCTAssertTrue([payload writeToURL:file atomically:YES]);

  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:file
                             phAssetLocalIdentifier:nil
                                        scanRootId:1
                                        generation:1
                                       displayName:@"dupbuster-stat-test.txt"
                                     mediaTypeHint:DBMediaTypeHintText
                                         sizeBytes:5
                                           mtimeNs:1000];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:folder.path mode:DBScanRootModeUserSelected];

  DBFakeFileStatReader *reader = [[DBFakeFileStatReader alloc] init];
  reader.fileStat =
      [[DBFileStat alloc] initWithSizeBytes:4096
                                    mtimeNs:2000000000
                                      inode:@42
                                   deviceId:@7
                                  isSymlink:NO];

  DBStatStage *stage =
      [[DBStatStage alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                               fileStatReader:reader];

  DBStatStageResult *result =
      [stage statDiscoveredEntry:entry scanRootGrant:grant provenance:DBUriProvenanceDiscovery];

  XCTAssertEqual(result.outcome, DBStatStageOutcomeSuccess);
  XCTAssertEqual(result.staged.sizeBytes, 4096);
  XCTAssertEqual(result.staged.mtimeNs, 2000000000);
  XCTAssertEqualObjects(result.staged.inode, @42);
  XCTAssertEqualObjects(result.staged.deviceId, @7);
  XCTAssertFalse(result.staged.isSymlink);
  XCTAssertEqualObjects(result.staged.mediaTypeHint, DBMediaTypeHintText);
}

- (void)testStatFileURL_detectsSymlinkViaReader
{
  NSURL *folder =
      [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
  NSURL *file = [folder URLByAppendingPathComponent:@"dupbuster-stat-link.txt"];

  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:file
                             phAssetLocalIdentifier:nil
                                        scanRootId:1
                                        generation:1
                                       displayName:@"link.txt"
                                     mediaTypeHint:DBMediaTypeHintText
                                         sizeBytes:0
                                           mtimeNs:0];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:folder.path mode:DBScanRootModeUserSelected];

  DBFakeFileStatReader *reader = [[DBFakeFileStatReader alloc] init];
  reader.fileStat =
      [[DBFileStat alloc] initWithSizeBytes:1 mtimeNs:1 inode:@1 deviceId:@1 isSymlink:YES];

  DBStatStage *stage =
      [[DBStatStage alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                               fileStatReader:reader];

  DBStatStageResult *result =
      [stage statDiscoveredEntry:entry scanRootGrant:grant provenance:DBUriProvenanceDiscovery];

  XCTAssertEqual(result.outcome, DBStatStageOutcomeSuccess);
  XCTAssertTrue(result.staged.isSymlink);
}

- (void)testStatPhAsset_returnsMtimeWithNullInode
{
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:nil
                             phAssetLocalIdentifier:@"A1B2C3D4-E5F6-7890-ABCD-EF1234567890"
                                        scanRootId:1
                                        generation:1
                                       displayName:@"photo.jpg"
                                     mediaTypeHint:DBMediaTypeHintImage
                                         sizeBytes:0
                                           mtimeNs:0];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];

  DBFakeFileStatReader *reader = [[DBFakeFileStatReader alloc] init];
  reader.phAssetStat =
      [[DBFileStat alloc] initWithSizeBytes:0
                                    mtimeNs:3000000000
                                      inode:nil
                                   deviceId:nil
                                  isSymlink:NO];

  DBStatStage *stage =
      [[DBStatStage alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                               fileStatReader:reader];

  DBStatStageResult *result =
      [stage statDiscoveredEntry:entry scanRootGrant:grant provenance:DBUriProvenanceDiscovery];

  XCTAssertEqual(result.outcome, DBStatStageOutcomeSuccess);
  XCTAssertNil(result.staged.inode);
  XCTAssertNil(result.staged.deviceId);
  XCTAssertEqual(result.staged.mtimeNs, 3000000000);
}

- (void)testStat_deniesOutOfGrantFileURL
{
  NSURL *file = [NSURL fileURLWithPath:@"/tmp/dupbuster-denied.txt"];
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:file
                             phAssetLocalIdentifier:nil
                                        scanRootId:1
                                        generation:1
                                       displayName:@"denied.txt"
                                     mediaTypeHint:DBMediaTypeHintText
                                         sizeBytes:0
                                           mtimeNs:0];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"/var/mobile/other"
                                           mode:DBScanRootModeUserSelected];

  DBFakeFileStatReader *reader = [[DBFakeFileStatReader alloc] init];
  reader.fileStat =
      [[DBFileStat alloc] initWithSizeBytes:1 mtimeNs:1 inode:@1 deviceId:@1 isSymlink:NO];

  DBStatStage *stage =
      [[DBStatStage alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                               fileStatReader:reader];

  DBStatStageResult *result =
      [stage statDiscoveredEntry:entry scanRootGrant:grant provenance:DBUriProvenanceDiscovery];

  XCTAssertEqual(result.outcome, DBStatStageOutcomeUnscannable);
  XCTAssertEqualObjects(result.unscannableReason, DBUnscannableReasonPermissionDenied);
}

@end
