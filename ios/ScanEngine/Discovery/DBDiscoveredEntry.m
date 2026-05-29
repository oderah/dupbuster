#import "DBDiscoveredEntry.h"

DBMediaTypeHint const DBMediaTypeHintImage = @"image";
DBMediaTypeHint const DBMediaTypeHintVideo = @"video";
DBMediaTypeHint const DBMediaTypeHintAudio = @"audio";
DBMediaTypeHint const DBMediaTypeHintDocument = @"document";
DBMediaTypeHint const DBMediaTypeHintText = @"text";
DBMediaTypeHint const DBMediaTypeHintOther = @"other";

@implementation DBDiscoveredEntry

- (instancetype)initWithContentURL:(NSURL *)contentURL
            phAssetLocalIdentifier:(NSString *)phAssetLocalIdentifier
                       scanRootId:(NSInteger)scanRootId
                       generation:(NSInteger)generation
                      displayName:(NSString *)displayName
                    mediaTypeHint:(DBMediaTypeHint)mediaTypeHint
                        sizeBytes:(int64_t)sizeBytes
                          mtimeNs:(int64_t)mtimeNs
{
  self = [super init];
  if (self) {
    _contentURL = [contentURL copy];
    _phAssetLocalIdentifier = [phAssetLocalIdentifier copy];
    _scanRootId = scanRootId;
    _generation = generation;
    _displayName = [displayName copy];
    _mediaTypeHint = [mediaTypeHint copy];
    _sizeBytes = sizeBytes;
    _mtimeNs = mtimeNs;
  }
  return self;
}

@end
