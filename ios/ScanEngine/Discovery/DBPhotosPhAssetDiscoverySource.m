#import "DBPhotosPhAssetDiscoverySource.h"

#import <Photos/Photos.h>

#import "DBMediaTypeHintResolver.h"

@implementation DBPhotosPhAssetDiscoverySource

- (NSArray<DBPhAssetRecord *> *)fetchAssetRecordsWithAuthorizedLocalIdentifiers:
    (NSArray<NSString *> *)authorizedLocalIdentifiers
{
  PHFetchResult<PHAsset *> *fetchResult = nil;
  if (authorizedLocalIdentifiers != nil) {
    fetchResult = [PHAsset fetchAssetsWithLocalIdentifiers:authorizedLocalIdentifiers options:nil];
  } else {
    fetchResult = [PHAsset fetchAssetsWithOptions:nil];
  }

  NSMutableArray<DBPhAssetRecord *> *records = [NSMutableArray arrayWithCapacity:fetchResult.count];
  [fetchResult enumerateObjectsUsingBlock:^(PHAsset *asset, NSUInteger idx, BOOL *stop) {
    (void)stop;
    DBMediaTypeHint hint = DBMediaTypeHintOther;
    switch (asset.mediaType) {
      case PHAssetMediaTypeImage:
        hint = DBMediaTypeHintImage;
        break;
      case PHAssetMediaTypeVideo:
        hint = DBMediaTypeHintVideo;
        break;
      case PHAssetMediaTypeAudio:
        hint = DBMediaTypeHintAudio;
        break;
      default:
        hint = DBMediaTypeHintOther;
        break;
    }

    int64_t mtimeNs = 0;
    if (asset.modificationDate != nil) {
      mtimeNs = (int64_t)(asset.modificationDate.timeIntervalSince1970 * 1e9);
    }

    NSString *displayName = asset.localIdentifier;
    NSArray<PHAssetResource *> *resources =
        [PHAssetResource assetResourcesForAsset:asset];
    for (PHAssetResource *resource in resources) {
      if (resource.originalFilename.length > 0) {
        displayName = resource.originalFilename;
        break;
      }
    }

    DBPhAssetRecord *record =
        [[DBPhAssetRecord alloc] initWithLocalIdentifier:asset.localIdentifier
                                           displayName:displayName
                                         mediaTypeHint:hint
                                             sizeBytes:0
                                               mtimeNs:mtimeNs];
    [records addObject:record];
  }];
  return records;
}

@end
