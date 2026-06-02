#import <XCTest/XCTest.h>

#import "DBPhAssetPlatformFileDeleter.h"
#import "DBScanRootGrant.h"

@interface DBPhAssetPlatformFileDeleterTests : XCTestCase
@end

@implementation DBPhAssetPlatformFileDeleterTests

- (void)testDeleteFileURL_removesFileUnderScopedGrant
{
  NSString *dir =
      [NSTemporaryDirectory() stringByAppendingPathComponent:@"dupbuster-delete-test"];
  [[NSFileManager defaultManager] createDirectoryAtPath:dir
                            withIntermediateDirectories:YES
                                             attributes:nil
                                                  error:nil];
  NSString *path = [dir stringByAppendingPathComponent:@"sample.txt"];
  XCTAssertTrue([@"dup" writeToFile:path atomically:YES encoding:NSUTF8StringEncoding error:nil]);

  NSURL *fileURL = [NSURL fileURLWithPath:path];
  NSURL *grantURL = [NSURL fileURLWithPath:dir];
  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:grantURL.absoluteString
                                           mode:DBScanRootModeUserSelected];

  DBPhAssetPlatformFileDeleter *deleter = [[DBPhAssetPlatformFileDeleter alloc] init];
  BOOL deleted = [deleter deleteURIString:fileURL.absoluteString scanRootGrant:grant];

  XCTAssertTrue(deleted);
  XCTAssertFalse([[NSFileManager defaultManager] fileExistsAtPath:path]);
  [[NSFileManager defaultManager] removeItemAtPath:dir error:nil];
}

@end
