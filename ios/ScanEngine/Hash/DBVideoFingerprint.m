#import "DBVideoFingerprint.h"

#import "DBSha256Hasher.h"

@implementation DBVideoFingerprint

- (instancetype)initWithFrameHashes:(NSArray<NSNumber *> *)frameHashes
                         durationMs:(int64_t)durationMs
                         videoWidth:(NSInteger)videoWidth
                        videoHeight:(NSInteger)videoHeight
{
  self = [super init];
  if (self) {
    _frameHashes = [frameHashes copy];
    _durationMs = durationMs;
    _videoWidth = videoWidth;
    _videoHeight = videoHeight;
  }
  return self;
}

- (NSString *)hashValue
{
  return [DBSha256Hasher hexDigestOfData:self.frameHashesBlob];
}

- (NSData *)frameHashesBlob
{
  NSMutableData *data = [NSMutableData dataWithCapacity:self.frameHashes.count * sizeof(uint64_t)];
  for (NSNumber *hash in self.frameHashes) {
    uint64_t value = hash.unsignedLongLongValue;
    [data appendBytes:&value length:sizeof(uint64_t)];
  }
  return data;
}

@end
