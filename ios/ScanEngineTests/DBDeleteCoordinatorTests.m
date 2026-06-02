#import <XCTest/XCTest.h>

#import "DBCatalogDatabase.h"
#import "DBDeleteCoordinator.h"
#import "DBDeleteDuplicatesCommand.h"
#import "DBDiscoveredEntry.h"
#import "DBMediaTypeHint.h"
#import "DBFileStat.h"
#import "DBGrouper.h"
#import "DBHashedFile.h"
#import "DBIndexWriter.h"
#import "DBNormalizationProfile.h"
#import "DBPlatformFileDeleter.h"
#import "DBScanRootGrant.h"
#import "DBStagedFile.h"
#import "DBUriValidator.h"

@interface DBFakePlatformFileDeleter : NSObject <DBPlatformFileDeleter>
@property (nonatomic, strong) NSMutableSet<NSString *> *deletedURIs;
@end

@implementation DBFakePlatformFileDeleter

- (instancetype)init
{
  self = [super init];
  if (self) {
    _deletedURIs = [NSMutableSet set];
  }
  return self;
}

- (BOOL)deleteURIString:(NSString *)uriString scanRootGrant:(DBScanRootGrant *)grant
{
  (void)grant;
  if (uriString.length == 0) {
    return NO;
  }
  [self.deletedURIs addObject:uriString];
  return YES;
}

@end

@interface DBDeleteCoordinatorTests : XCTestCase
@property (nonatomic, strong) DBCatalogDatabase *database;
@property (nonatomic, strong) DBIndexWriter *writer;
@property (nonatomic, strong) DBGrouper *grouper;
@property (nonatomic, assign) NSInteger rootId;
@end

@implementation DBDeleteCoordinatorTests

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

- (void)testRunDelete_withFakeDeleter_updatesCatalogAndReturnsCounts
{
  NSString *uriA = @"file:///docs/photos/a.jpg";
  NSString *uriB = @"file:///docs/photos/b.jpg";
  NSInteger idA = [self upsertHashed:uriA hash:@"hash-dup"];
  NSInteger idB = [self upsertHashed:uriB hash:@"hash-dup"];
  [self.grouper rebuildDuplicateGroups];
  NSInteger groupId = [self.grouper firstDuplicateGroupId];

  DBFakePlatformFileDeleter *fakeDeleter = [[DBFakePlatformFileDeleter alloc] init];
  DBDeleteCoordinator *coordinator =
      [[DBDeleteCoordinator alloc] initWithIndexWriter:self.writer
                                          uriValidator:[[DBUriValidator alloc] init]
                                    platformFileDeleter:fakeDeleter
                                             workQueue:nil];

  DBDeleteDuplicatesCommand *command = [[DBDeleteDuplicatesCommand alloc] init];
  command.groupId = groupId;
  command.keeperFileEntryId = idA;
  command.deleteFileEntryIds = @[ @(idB) ];

  NSError *error = nil;
  DBDeleteDuplicatesResult *result = [coordinator runDelete:command error:&error];

  XCTAssertNil(error);
  XCTAssertEqual(result.deletedCount, 1);
  XCTAssertEqual(result.failedCount, 0);
  XCTAssertTrue([fakeDeleter.deletedURIs containsObject:uriB]);
  XCTAssertNil([self.writer memberFileEntryIdsForGroupId:groupId]);

  NSString *keeperUri = nil;
  DBScanRootGrant *grant = nil;
  XCTAssertTrue([self.writer loadDeleteTargetForFileEntryId:idA
                                                  uriString:&keeperUri
                                                      grant:&grant]);
  XCTAssertNotNil(keeperUri);
}

- (void)testRunDelete_pendingPlatformDeleter_reportsFailure
{
  NSString *uriA = @"file:///docs/photos/a.jpg";
  NSString *uriB = @"file:///docs/photos/b.jpg";
  NSInteger idA = [self upsertHashed:uriA hash:@"hash-dup"];
  NSInteger idB = [self upsertHashed:uriB hash:@"hash-dup"];
  [self.grouper rebuildDuplicateGroups];
  NSInteger groupId = [self.grouper firstDuplicateGroupId];

  DBDeleteCoordinator *coordinator =
      [[DBDeleteCoordinator alloc] initWithIndexWriter:self.writer
                                          uriValidator:[[DBUriValidator alloc] init]
                                    platformFileDeleter:[[DBPendingPlatformFileDeleter alloc] init]
                                             workQueue:nil];

  DBDeleteDuplicatesCommand *command = [[DBDeleteDuplicatesCommand alloc] init];
  command.groupId = groupId;
  command.keeperFileEntryId = idA;
  command.deleteFileEntryIds = @[ @(idB) ];

  DBDeleteDuplicatesResult *result = [coordinator runDelete:command error:nil];

  XCTAssertEqual(result.deletedCount, 0);
  XCTAssertEqual(result.failedCount, 1);
  XCTAssertNotNil([self.writer memberFileEntryIdsForGroupId:groupId]);
}

- (NSInteger)upsertHashed:(NSString *)uri hash:(NSString *)hash
{
  DBStagedFile *staged = [self stagedWithUri:uri];
  DBHashedFile *hashed =
      [[DBHashedFile alloc] initWithStaged:staged
                                 hashValue:hash
                      normalizationProfile:DBNormalizationProfileRawBytes
                           quickSampleHash:nil];
  return [self.writer upsertHashedFile:hashed generation:1];
}

- (DBStagedFile *)stagedWithUri:(NSString *)uri
{
  NSURL *url = [NSURL URLWithString:uri];
  DBDiscoveredEntry *entry =
      [[DBDiscoveredEntry alloc] initWithContentURL:url
                             phAssetLocalIdentifier:nil
                                         scanRootId:self.rootId
                                         generation:1
                                        displayName:@"photo.jpg"
                                      mediaTypeHint:DBMediaTypeHintImage
                                          sizeBytes:200
                                            mtimeNs:2000000000];
  DBFileStat *stat =
      [[DBFileStat alloc] initWithSizeBytes:200
                                    mtimeNs:2000000000
                                      inode:nil
                                   deviceId:nil
                                 isSymlink:NO];
  return [[DBStagedFile alloc] initWithDiscovered:entry fileStat:stat];
}

@end
