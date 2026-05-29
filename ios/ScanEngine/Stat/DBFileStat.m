#import "DBFileStat.h"

@implementation DBFileStat

- (instancetype)initWithSizeBytes:(int64_t)sizeBytes
                          mtimeNs:(int64_t)mtimeNs
                            inode:(NSNumber *)inode
                         deviceId:(NSNumber *)deviceId
                        isSymlink:(BOOL)isSymlink
{
  self = [super init];
  if (self) {
    _sizeBytes = sizeBytes;
    _mtimeNs = mtimeNs;
    _inode = inode;
    _deviceId = deviceId;
    _isSymlink = isSymlink;
  }
  return self;
}

@end
