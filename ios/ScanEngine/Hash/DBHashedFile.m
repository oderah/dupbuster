#import "DBHashedFile.h"

@implementation DBHashedFile

- (instancetype)initWithStaged:(DBStagedFile *)staged
                     hashValue:(NSString *)hashValue
          normalizationProfile:(NSString *)normalizationProfile
               quickSampleHash:(NSString *)quickSampleHash
{
  self = [super init];
  if (self) {
    _staged = staged;
    _hashValue = [hashValue copy];
    _normalizationProfile = [normalizationProfile copy];
    _quickSampleHash = [quickSampleHash copy];
  }
  return self;
}

@end
