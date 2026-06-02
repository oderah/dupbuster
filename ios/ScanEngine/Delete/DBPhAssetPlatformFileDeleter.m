#import "DBPhAssetPlatformFileDeleter.h"

#import <Photos/Photos.h>

#import "DBScanRootGrant.h"

@implementation DBPhAssetPlatformFileDeleter

- (BOOL)deleteURIString:(NSString *)uriString scanRootGrant:(DBScanRootGrant *)grant
{
  if (uriString.length == 0) {
    return NO;
  }
  if ([uriString hasPrefix:@"file://"]) {
    return [self deleteFileURLString:uriString scanRootGrant:grant];
  }
  return [self deletePhAssetLocalIdentifier:uriString];
}

- (BOOL)deletePhAssetLocalIdentifier:(NSString *)localIdentifier
{
  PHAuthorizationStatus status =
      [PHPhotoLibrary authorizationStatusForAccessLevel:PHAccessLevelReadWrite];
  if (status != PHAuthorizationStatusAuthorized && status != PHAuthorizationStatusLimited) {
    return NO;
  }

  PHFetchResult<PHAsset *> *fetchResult =
      [PHAsset fetchAssetsWithLocalIdentifiers:@[ localIdentifier ] options:nil];
  PHAsset *asset = fetchResult.firstObject;
  if (asset == nil) {
    return NO;
  }

  NSError *changeError = nil;
  BOOL changed = [[PHPhotoLibrary sharedPhotoLibrary] performChangesAndWait:^{
    [PHAssetChangeRequest deleteAssets:@[ asset ]];
  }
                                                                       error:&changeError];
  return changed && changeError == nil;
}

- (BOOL)deleteFileURLString:(NSString *)uriString scanRootGrant:(DBScanRootGrant *)grant
{
  NSURL *fileURL = [NSURL URLWithString:uriString];
  if (fileURL == nil || !fileURL.isFileURL) {
    return NO;
  }

  BOOL grantAccessed = NO;
  NSURL *grantURL = grant.uriGrant.length > 0 ? [NSURL URLWithString:grant.uriGrant] : nil;
  if (grant.mode == DBScanRootModeUserSelected && grantURL != nil && grantURL.isFileURL) {
    grantAccessed = [grantURL startAccessingSecurityScopedResource];
  }

  @try {
    NSFileCoordinator *coordinator = [[NSFileCoordinator alloc] init];
    __block NSError *coordError = nil;
    __block BOOL removed = NO;
    [coordinator coordinateWritingItemAtURL:fileURL
                                    options:NSFileCoordinatorWritingForDeleting
                                      error:&coordError
                                 byAccessor:^(NSURL *newURL) {
                                   NSError *removeError = nil;
                                   removed = [[NSFileManager defaultManager] removeItemAtURL:newURL
                                                                                       error:&removeError];
                                   if (!removed) {
                                     coordError = removeError;
                                   }
                                 }];
    return removed && coordError == nil;
  } @finally {
    if (grantAccessed) {
      [grantURL stopAccessingSecurityScopedResource];
    }
  }
}

@end
