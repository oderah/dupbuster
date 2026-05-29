#import <Foundation/Foundation.h>

#import "DBStagedFile.h"

NS_ASSUME_NONNULL_BEGIN

@interface DBHashedFile : NSObject

@property (nonatomic, strong) DBStagedFile *staged;
@property (nonatomic, copy) NSString *hashValue;
@property (nonatomic, copy) NSString *normalizationProfile;
@property (nonatomic, copy, nullable) NSString *quickSampleHash;
@property (nonatomic, copy, nullable) NSData *frameHashesBlob;

- (instancetype)initWithStaged:(DBStagedFile *)staged
                     hashValue:(NSString *)hashValue
          normalizationProfile:(NSString *)normalizationProfile
               quickSampleHash:(nullable NSString *)quickSampleHash
              frameHashesBlob:(nullable NSData *)frameHashesBlob;

@end

NS_ASSUME_NONNULL_END
