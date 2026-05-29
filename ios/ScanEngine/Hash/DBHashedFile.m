#import "DBHashedFile.h"

@implementation DBHashedFile

- (instancetype)initWithStaged:(DBStagedFile *)staged
                     hashValue:(NSString *)hashValue
          normalizationProfile:(NSString *)normalizationProfile
               quickSampleHash:(NSString *)quickSampleHash
{
  return [self initWithStaged:staged
                    hashValue:hashValue
         normalizationProfile:normalizationProfile
              quickSampleHash:quickSampleHash
             frameHashesBlob:nil];
}

- (instancetype)initWithStaged:(DBStagedFile *)staged
                     hashValue:(NSString *)hashValue
          normalizationProfile:(NSString *)normalizationProfile
               quickSampleHash:(NSString *)quickSampleHash
              frameHashesBlob:(NSData *)frameHashesBlob
{
  self = [super init];
  if (self) {
    _staged = staged;
    _hashValue = [hashValue copy];
    _normalizationProfile = [normalizationProfile copy];
    _quickSampleHash = [quickSampleHash copy];
    _frameHashesBlob = [frameHashesBlob copy];
  }
  return self;
}

@end
