#import "DBPhAssetRecord.h"

@implementation DBPhAssetRecord

- (instancetype)initWithLocalIdentifier:(NSString *)localIdentifier
                            displayName:(NSString *)displayName
                          mediaTypeHint:(DBMediaTypeHint)mediaTypeHint
                              sizeBytes:(int64_t)sizeBytes
                                mtimeNs:(int64_t)mtimeNs
{
  self = [super init];
  if (self) {
    _localIdentifier = [localIdentifier copy];
    _displayName = [displayName copy];
    _mediaTypeHint = [mediaTypeHint copy];
    _sizeBytes = sizeBytes;
    _mtimeNs = mtimeNs;
  }
  return self;
}

@end
