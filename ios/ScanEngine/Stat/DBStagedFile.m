#import "DBStagedFile.h"

@implementation DBStagedFile

- (instancetype)initWithDiscovered:(DBDiscoveredEntry *)discovered fileStat:(DBFileStat *)fileStat
{
  self = [super init];
  if (self) {
    _discovered = discovered;
    _sizeBytes = fileStat.sizeBytes;
    _mtimeNs = fileStat.mtimeNs;
    _inode = fileStat.inode;
    _deviceId = fileStat.deviceId;
    _isSymlink = fileStat.isSymlink;
    _mediaTypeHint = [discovered.mediaTypeHint copy];
  }
  return self;
}

@end
