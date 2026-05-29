#import "DBFileStat.h"

@implementation DBFileStat

- (instancetype)initWithSizeBytes:(int64_t)sizeBytes
                          mtimeNs:(int64_t)mtimeNs
                            inode:(NSNumber *)inode
                         deviceId:(NSNumber *)deviceId
                        isSymlink:(BOOL)isSymlink
{
  return [self initWithSizeBytes:sizeBytes
                         mtimeNs:mtimeNs
                           inode:inode
                        deviceId:deviceId
                       isSymlink:isSymlink
                      durationMs:0
                      videoWidth:0
                     videoHeight:0];
}

- (instancetype)initWithSizeBytes:(int64_t)sizeBytes
                          mtimeNs:(int64_t)mtimeNs
                            inode:(NSNumber *)inode
                         deviceId:(NSNumber *)deviceId
                        isSymlink:(BOOL)isSymlink
                       durationMs:(int64_t)durationMs
                       videoWidth:(NSInteger)videoWidth
                      videoHeight:(NSInteger)videoHeight
{
  self = [super init];
  if (self) {
    _sizeBytes = sizeBytes;
    _mtimeNs = mtimeNs;
    _inode = inode;
    _deviceId = deviceId;
    _isSymlink = isSymlink;
    _durationMs = durationMs;
    _videoWidth = videoWidth;
    _videoHeight = videoHeight;
  }
  return self;
}

@end
