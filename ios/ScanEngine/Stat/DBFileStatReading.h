#import <Foundation/Foundation.h>

#import "DBFileStat.h"

NS_ASSUME_NONNULL_BEGIN

@protocol DBFileStatReading <NSObject>

- (nullable DBFileStat *)statFileURL:(NSURL *)fileURL error:(NSError **)error;

- (nullable DBFileStat *)statPhAssetLocalIdentifier:(NSString *)localIdentifier error:(NSError **)error;

@end

NS_ASSUME_NONNULL_END
