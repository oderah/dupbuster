#import <XCTest/XCTest.h>

#import "DBDiscoveryEmitter.h"
#import "DBPhAssetRecord.h"
#import "DBUriValidator.h"

@interface DBFakePhAssetDiscoverySource : NSObject <DBPhAssetDiscoverySource>
@property (nonatomic, copy) NSArray<DBPhAssetRecord *> *records;
@end

@implementation DBFakePhAssetDiscoverySource

- (NSArray<DBPhAssetRecord *> *)fetchAssetRecordsWithAuthorizedLocalIdentifiers:
    (NSArray<NSString *> *)authorizedLocalIdentifiers
{
  (void)authorizedLocalIdentifiers;
  return self.records ?: @[];
}

@end

@interface DBDiscoveryEmitterModeBTests : XCTestCase
@end

@implementation DBDiscoveryEmitterModeBTests

- (void)testEmitModeB_emitsValidatedPhAssets
{
  DBPhAssetRecord *image =
      [[DBPhAssetRecord alloc] initWithLocalIdentifier:@"A1B2C3D4-E5F6-7890-ABCD-EF1234567890"
                                         displayName:@"photo.jpg"
                                       mediaTypeHint:DBMediaTypeHintImage
                                           sizeBytes:1024
                                             mtimeNs:1000];
  DBPhAssetRecord *video =
      [[DBPhAssetRecord alloc] initWithLocalIdentifier:@"B2C3D4E5-F6A7-8901-BCDE-F12345678901"
                                         displayName:@"clip.mp4"
                                       mediaTypeHint:DBMediaTypeHintVideo
                                           sizeBytes:2048
                                             mtimeNs:2000];
  DBFakePhAssetDiscoverySource *source = [[DBFakePhAssetDiscoverySource alloc] init];
  source.records = @[ image, video ];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];
  DBDiscoveryEmitter *emitter =
      [[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                              phAssetDiscoverySource:source];

  NSMutableArray<NSString *> *names = [NSMutableArray array];
  DBDiscoveryResult *result =
      [emitter emitModeBWithScanRootGrant:grant
                             scanRootId:5
                             generation:4
             authorizedLocalIdentifiers:nil
           additionalScopedFolderURLs:nil
           additionalScopedGrants:nil
                                handler:^(DBDiscoveredEntry *entry) {
                                  [names addObject:entry.displayName];
                                  XCTAssertNotNil(entry.phAssetLocalIdentifier);
                                  XCTAssertNil(entry.contentURL);
                                }
                            isCancelled:nil];

  XCTAssertEqual(result.entriesEmitted, 2);
  XCTAssertEqual(result.entriesDenied, 0);
  XCTAssertTrue([names containsObject:@"photo.jpg"]);
  XCTAssertTrue([names containsObject:@"clip.mp4"]);
}

- (void)testEmitModeB_deniesInvalidLocalIdentifier
{
  DBPhAssetRecord *bad =
      [[DBPhAssetRecord alloc] initWithLocalIdentifier:@"bad/../id"
                                         displayName:@"evil.jpg"
                                       mediaTypeHint:DBMediaTypeHintImage
                                           sizeBytes:1
                                             mtimeNs:0];
  DBFakePhAssetDiscoverySource *source = [[DBFakePhAssetDiscoverySource alloc] init];
  source.records = @[ bad ];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];
  DBDiscoveryEmitter *emitter =
      [[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                              phAssetDiscoverySource:source];

  DBDiscoveryResult *result =
      [emitter emitModeBWithScanRootGrant:grant
                             scanRootId:1
                             generation:1
             authorizedLocalIdentifiers:nil
           additionalScopedFolderURLs:nil
           additionalScopedGrants:nil
                                handler:^(__unused DBDiscoveredEntry *entry) {
                                }
                            isCancelled:nil];

  XCTAssertEqual(result.entriesEmitted, 0);
  XCTAssertEqual(result.entriesDenied, 1);
}

- (void)testEmitModeB_honoursCancel
{
  DBPhAssetRecord *image =
      [[DBPhAssetRecord alloc] initWithLocalIdentifier:@"A1B2C3D4-E5F6-7890-ABCD-EF1234567890"
                                         displayName:@"photo.jpg"
                                       mediaTypeHint:DBMediaTypeHintImage
                                           sizeBytes:1
                                             mtimeNs:0];
  DBFakePhAssetDiscoverySource *source = [[DBFakePhAssetDiscoverySource alloc] init];
  source.records = @[ image ];

  DBScanRootGrant *grant =
      [[DBScanRootGrant alloc] initWithUriGrant:@"*" mode:DBScanRootModePlatformDiscovery];
  DBDiscoveryEmitter *emitter =
      [[DBDiscoveryEmitter alloc] initWithUriValidator:[[DBUriValidator alloc] init]
                              phAssetDiscoverySource:source];

  DBDiscoveryResult *result =
      [emitter emitModeBWithScanRootGrant:grant
                             scanRootId:1
                             generation:1
             authorizedLocalIdentifiers:nil
           additionalScopedFolderURLs:nil
           additionalScopedGrants:nil
                                handler:^(__unused DBDiscoveredEntry *entry) {
                                }
                            isCancelled:^BOOL {
                              return YES;
                            }];

  XCTAssertTrue(result.cancelled);
  XCTAssertEqual(result.entriesEmitted, 0);
}

@end
