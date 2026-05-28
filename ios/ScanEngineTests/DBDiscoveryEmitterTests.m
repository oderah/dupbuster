#import <XCTest/XCTest.h>

#import "DBDiscoveryEmitter.h"
#import "DBUriValidator.h"

@interface DBDiscoveryEmitterTests : XCTestCase
@property (nonatomic, copy) NSString *tempRoot;
@end

@implementation DBDiscoveryEmitterTests

- (void)setUp
{
  [super setUp];
  self.tempRoot =
      [NSTemporaryDirectory() stringByAppendingPathComponent:[[NSUUID UUID] UUIDString]];
  [[NSFileManager defaultManager] createDirectoryAtPath:self.tempRoot
                            withIntermediateDirectories:YES
                                             attributes:nil
                                                  error:nil];
}

- (void)tearDown
{
  [[NSFileManager defaultManager] removeItemAtPath:self.tempRoot error:nil];
  [super tearDown];
}

- (void)testEmitModeA_enumeratesNestedFiles
{
  NSString *photos = [self.tempRoot stringByAppendingPathComponent:@"photos"];
  [[NSFileManager defaultManager] createDirectoryAtPath:photos
                            withIntermediateDirectories:YES
                                             attributes:nil
                                                  error:nil];
  NSString *readme = [self.tempRoot stringByAppendingPathComponent:@"readme.txt"];
  [@"hi" writeToFile:readme atomically:YES encoding:NSUTF8StringEncoding error:nil];
  NSString *photo = [photos stringByAppendingPathComponent:@"a.jpg"];
  [@"" writeToFile:photo atomically:YES encoding:NSUTF8StringEncoding error:nil];

  NSURL *rootURL = [NSURL fileURLWithPath:self.tempRoot isDirectory:YES];
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:rootURL.path mode:DBScanRootModeUserSelected];
  DBDiscoveryEmitter *emitter = [[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]];

  NSMutableArray<NSString *> *names = [NSMutableArray array];
  DBDiscoveryResult *result =
      [emitter emitModeAWithFolderURL:rootURL
                        scanRootGrant:grant
                           scanRootId:1
                           generation:3
                              handler:^(DBDiscoveredEntry *entry) {
                                [names addObject:entry.displayName];
                              }
                            isCancelled:nil];

  XCTAssertEqual(result.entriesEmitted, 2);
  XCTAssertEqual(result.entriesDenied, 0);
  XCTAssertTrue([names containsObject:@"readme.txt"]);
  XCTAssertTrue([names containsObject:@"a.jpg"]);
}

- (void)testEmitModeA_deniesPathOutsideGrant
{
  NSString *nested = [self.tempRoot stringByAppendingPathComponent:@"nested"];
  [[NSFileManager defaultManager] createDirectoryAtPath:nested
                            withIntermediateDirectories:YES
                                             attributes:nil
                                                  error:nil];
  NSString *rootFile = [self.tempRoot stringByAppendingPathComponent:@"outside.txt"];
  [@"x" writeToFile:rootFile atomically:YES encoding:NSUTF8StringEncoding error:nil];
  NSString *nestedFile = [nested stringByAppendingPathComponent:@"inside.txt"];
  [@"y" writeToFile:nestedFile atomically:YES encoding:NSUTF8StringEncoding error:nil];

  NSURL *rootURL = [NSURL fileURLWithPath:self.tempRoot isDirectory:YES];
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:nested mode:DBScanRootModeUserSelected];
  DBDiscoveryEmitter *emitter = [[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]];

  DBDiscoveryResult *result =
      [emitter emitModeAWithFolderURL:rootURL
                        scanRootGrant:grant
                           scanRootId:1
                           generation:1
                              handler:^(DBDiscoveredEntry *entry) {
                                (void)entry;
                              }
                            isCancelled:nil];

  XCTAssertEqual(result.entriesEmitted, 1);
  XCTAssertEqual(result.entriesDenied, 1);
}

- (void)testEmitModeA_honoursCancel
{
  NSURL *rootURL = [NSURL fileURLWithPath:self.tempRoot isDirectory:YES];
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:rootURL.path mode:DBScanRootModeUserSelected];
  DBDiscoveryEmitter *emitter = [[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]];

  DBDiscoveryResult *result =
      [emitter emitModeAWithFolderURL:rootURL
                        scanRootGrant:grant
                           scanRootId:1
                           generation:1
                              handler:^(DBDiscoveredEntry *entry) {
                                (void)entry;
                              }
                            isCancelled:^BOOL {
                              return YES;
                            }];

  XCTAssertTrue(result.cancelled);
  XCTAssertEqual(result.entriesEmitted, 0);
}

@end
