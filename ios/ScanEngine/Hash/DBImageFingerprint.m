#import "DBImageFingerprint.h"

#import "DBSha256Hasher.h"

@implementation DBImageFingerprint

- (instancetype)initWithDHash:(uint64_t)dHash
{
  self = [super init];
  if (self) {
    _dHash = dHash;
  }
  return self;
}

- (NSString *)hashValue
{
  return [DBSha256Hasher hexDigestOfData:self.frameHashesBlob];
}

- (NSData *)frameHashesBlob
{
  uint64_t value = self.dHash;
  return [NSData dataWithBytes:&value length:sizeof(uint64_t)];
}

@end
