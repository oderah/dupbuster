#import "DBFileStatReader.h"

#import <Photos/Photos.h>
#import <sys/stat.h>

static int64_t DBMtimeNsFromTimespec(const struct timespec *ts)
{
  return (int64_t)ts->tv_sec * 1000000000LL + (int64_t)ts->tv_nsec;
}

@implementation DBFileStatReader

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error
{
  if (fileURL == nil || !fileURL.isFileURL) {
    if (error != nil) {
      *error = [NSError errorWithDomain:NSCocoaErrorDomain
                                   code:NSFileReadInvalidFileNameError
                               userInfo:nil];
    }
    return nil;
  }

  const char *path = fileURL.fileSystemRepresentation;
  struct stat linkStat;
  if (lstat(path, &linkStat) != 0) {
    if (error != nil) {
      *error = [NSError errorWithDomain:NSPOSIXErrorDomain code:errno userInfo:nil];
    }
    return nil;
  }

  BOOL isSymlink = S_ISLNK(linkStat.st_mode) != 0;
  // lstat only — do not follow symlinks (requirements §4 / AC-equiv-symlink-01).
  int64_t mtimeNs = DBMtimeNsFromTimespec(&linkStat.st_mtimespec);
  return [[DBFileStat alloc] initWithSizeBytes:(int64_t)linkStat.st_size
                                       mtimeNs:mtimeNs
                                         inode:@((long long)linkStat.st_ino)
                                      deviceId:@((long long)linkStat.st_dev)
                                     isSymlink:isSymlink];
}

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error
{
  PHFetchResult<PHAsset *> *fetchResult =
      [PHAsset fetchAssetsWithLocalIdentifiers:@[ localIdentifier ] options:nil];
  PHAsset *asset = fetchResult.firstObject;
  if (asset == nil) {
    if (error != nil) {
      *error = [NSError errorWithDomain:NSCocoaErrorDomain
                                   code:NSFileNoSuchFileError
                               userInfo:nil];
    }
    return nil;
  }

  int64_t mtimeNs = 0;
  if (asset.modificationDate != nil) {
    mtimeNs = (int64_t)(asset.modificationDate.timeIntervalSince1970 * 1e9);
  }

  // PHAsset library items have no stable inode/device_id for hard-link detection (FR-IX-04).
  return [[DBFileStat alloc] initWithSizeBytes:0
                                       mtimeNs:mtimeNs
                                         inode:nil
                                      deviceId:nil
                                     isSymlink:NO];
}

@end
